MODULE Kernel;  (*NW/PR  11.4.86 / 27.12.95 / 4.2.2014*)
  IMPORT SYSTEM;
  CONST SectorLength* = 1024;
    hwEnumerator* = -4;
    CARD0 = 1; SPIFAST = 4;
    FSoffset = 80000H; (*256MB in 512-byte blocks*)
    mapsize = 10000H; (*1K sectors, 64MB*)

  TYPE Sector* = ARRAY SectorLength OF BYTE;
    WeakReference* = POINTER TO WeakReferenceDesc;
    WeakReferenceDesc* = RECORD
      nextRef: WeakReference;
      target*: INTEGER;
    END;

  VAR allocated*, NofSectors*: INTEGER;
    weakRefRoot: WeakReference;
    heapOrg*, heapLim*: INTEGER; 
    stackOrg* ,  stackSize*, MemLim*: INTEGER;
    clock, clockTime: INTEGER;
    list0, list1, list2, list3: INTEGER;  (*lists of free blocks of size n*256, 128, 64, 32 bytes*)
    data: INTEGER; (*SPI data in*)
    sectorMapStart*, lastUsedSector: INTEGER;
    sectorMap: ARRAY mapsize DIV 32 OF SET;
    timer, spiData, spiCtrl, pvDisk, cacheInvalAddr, dbgConsole, softResetAddr, hardResetAddr, quitAddr: INTEGER;
    powerManagement: BOOLEAN;
    
(* ---------- New: heap allocation ----------*)

  PROCEDURE GetBlock(VAR p: LONGINT; len: LONGINT);
    (*len is multiple of 256*)
    VAR q0, q1, q2, size: LONGINT; done: BOOLEAN;
  BEGIN q0 := 0; q1 := list0; done := FALSE;
    WHILE ~done & (q1 # 0) DO
      SYSTEM.GET(q1, size); SYSTEM.GET(q1+8, q2);
      IF size < len THEN (*no fit*) q0 := q1; q1 := q2
      ELSIF size = len THEN (*extract -> p*)
        done := TRUE; p := q1;
        IF q0 # 0 THEN SYSTEM.PUT(q0+8, q2) ELSE list0 := q2 END
      ELSE (*reduce size*)
        done := TRUE; p := q1; q1 := q1 + len;
        SYSTEM.PUT(q1, size-len); SYSTEM.PUT(q1+4, -1); SYSTEM.PUT(q1+8, q2);
        IF q0 # 0 THEN SYSTEM.PUT(q0+8, q1) ELSE list0 := q1 END
      END
    END ;
    IF ~done THEN p := 0 END
  END GetBlock;

  PROCEDURE GetBlock128(VAR p: LONGINT);
    VAR q: LONGINT;
  BEGIN
    IF list1 # 0 THEN p := list1; SYSTEM.GET(list1+8, list1)
    ELSE GetBlock(q, 256);
      IF q # 0 THEN
        SYSTEM.PUT(q+128, 128); SYSTEM.PUT(q+132, -1); SYSTEM.PUT(q+136, list1);
        list1 := q + 128
      END;
      p := q
    END
  END GetBlock128;

  PROCEDURE GetBlock64(VAR p: LONGINT);
    VAR q: LONGINT;
  BEGIN
    IF list2 # 0 THEN p := list2; SYSTEM.GET(list2+8, list2)
    ELSE GetBlock128(q);
      IF q # 0 THEN
        SYSTEM.PUT(q+64, 64); SYSTEM.PUT(q+68, -1); SYSTEM.PUT(q+72, list2);
        list2 := q + 64
      END;
      p := q
    END
  END GetBlock64;

  PROCEDURE GetBlock32(VAR p: LONGINT);
    VAR q: LONGINT;
  BEGIN
    IF list3 # 0 THEN p := list3; SYSTEM.GET(list3+8, list3)
    ELSE GetBlock64(q);
      IF q # 0 THEN
        SYSTEM.PUT(q+32, 32); SYSTEM.PUT(q+36, -1); SYSTEM.PUT(q+40, list3);
        list3 := q + 32
      END;
      p := q
    END
  END GetBlock32;

   PROCEDURE New*(VAR ptr: LONGINT; tag: LONGINT);
    (*called by NEW via MT[0]; ptr and tag are pointers*)
    VAR p, size, lim: LONGINT;
  BEGIN SYSTEM.GET(tag, size);
    IF size = 32 THEN GetBlock32(p)
    ELSIF size = 64 THEN GetBlock64(p)
    ELSIF size = 128 THEN GetBlock128(p)
    ELSE GetBlock(p, (size+255) DIV 256 * 256)
    END ;
    IF p = 0 THEN ptr := 0
    ELSE ptr := p+8; SYSTEM.PUT(p, tag); lim := p + size; INC(p, 4); INC(allocated, size);
      WHILE p < lim DO SYSTEM.PUT(p, 0); INC(p, 4) END
    END
  END New;

(* ---------- Garbage collector ----------*)

  PROCEDURE Mark*(pref: LONGINT);
    VAR pvadr, offadr, offset, tag, p, q, r: LONGINT;
  BEGIN SYSTEM.GET(pref, pvadr); (*pointers < heapOrg considered NIL*)
    WHILE pvadr # 0 DO
      SYSTEM.GET(pvadr, p); IF p >= heapOrg THEN SYSTEM.GET(p-4, offadr) END;
      IF (p >= heapOrg) & (offadr = 0) THEN q := p;   (*mark elements in data structure with root p*)
        REPEAT SYSTEM.GET(p-4, offadr);
          IF offadr = 0 THEN SYSTEM.GET(p-8, tag); offadr := tag + 16 ELSE INC(offadr, 4) END ;
          SYSTEM.PUT(p-4, offadr); SYSTEM.GET(offadr, offset);
          IF offset # -1 THEN (*down*)
            SYSTEM.GET(p+offset, r); IF r >= heapOrg THEN SYSTEM.GET(r-4, offadr) END;
            IF (r >= heapOrg) & (offadr = 0) THEN SYSTEM.PUT(p+offset, q); q := p; p := r END
          ELSE (*up*) SYSTEM.GET(q-4, offadr); SYSTEM.GET(offadr, offset);
            IF p # q THEN SYSTEM.GET(q+offset, r); SYSTEM.PUT(q+offset, p); p := q; q := r END
          END
        UNTIL (p = q) & (offset = -1)
      END ;
      INC(pref, 4); SYSTEM.GET(pref, pvadr)
    END
  END Mark;

  PROCEDURE Scan*;
    VAR p, q, mark, tag, size: LONGINT;
  BEGIN p := heapOrg;
    REPEAT SYSTEM.GET(p+4, mark); q := p;
      WHILE mark = 0 DO
        SYSTEM.GET(p, tag); SYSTEM.GET(tag, size); INC(p, size); SYSTEM.GET(p+4, mark)
      END ;
      size := p - q; DEC(allocated, size);  (*size of free block*)
      IF size > 0 THEN
        IF size MOD 64 # 0 THEN
          SYSTEM.PUT(q, 32); SYSTEM.PUT(q+4, -1); SYSTEM.PUT(q+8, list3); list3 := q; INC(q, 32); DEC(size, 32)
        END ;
        IF size MOD 128 # 0 THEN
          SYSTEM.PUT(q, 64); SYSTEM.PUT(q+4, -1); SYSTEM.PUT(q+8, list2); list2 := q; INC(q, 64); DEC(size, 64)
        END ;
        IF size MOD 256 # 0 THEN
          SYSTEM.PUT(q, 128); SYSTEM.PUT(q+4, -1); SYSTEM.PUT(q+8,  list1); list1 := q; INC(q, 128); DEC(size, 128)
        END ;
        IF size > 0 THEN
          SYSTEM.PUT(q, size); SYSTEM.PUT(q+4, -1); SYSTEM.PUT(q+8, list0); list0 := q; INC(q, size)
        END
      END ;
      IF mark > 0 THEN SYSTEM.GET(p, tag); SYSTEM.GET(tag, size); SYSTEM.PUT(p+4, 0); INC(p, size)
      ELSE (*free*) SYSTEM.GET(p, size); INC(p, size)
      END
    UNTIL p >= heapLim
  END Scan;

(* ---------- Debug console ----------*)

  PROCEDURE ConsoleCh*(ch: CHAR);
  BEGIN
    IF dbgConsole # 0 THEN SYSTEM.PUT(dbgConsole, ch) END
  END ConsoleCh;

  PROCEDURE ConsoleHex*(val: INTEGER);
    VAR i, d: INTEGER;
  BEGIN
    FOR i := 0 TO 7 DO
      val := ROR(val, 28);
      d := val MOD 10H;
      IF d >= 10 THEN INC(d, 7) END;
      ConsoleCh(CHR(d + 30H));
    END;
  END ConsoleHex;

  PROCEDURE ConsoleStr*(str: ARRAY OF CHAR);
    VAR i: INTEGER;
  BEGIN i := 0;
    WHILE str[i] # 0X DO ConsoleCh(str[i]); INC(i) END
  END ConsoleStr;

  PROCEDURE ConsoleLn*();
    VAR i: INTEGER;
  BEGIN ConsoleCh(0DX);
  END ConsoleLn;

  PROCEDURE ConsoleFlush*();
    VAR i: INTEGER;
  BEGIN ConsoleCh(0X);
  END ConsoleFlush;

(* ---------- Hardware enumerator ----------*)

  PROCEDURE ToHardwareID*(str: ARRAY OF CHAR): INTEGER;
  BEGIN
  RETURN ORD(str[0]) * 1000000H + ORD(str[1]) * 10000H + ORD(str[2]) * 100H + ORD(str[3])
  END ToHardwareID;

  PROCEDURE PowerManagementSleep*(until: INTEGER);
  BEGIN IF powerManagement THEN SYSTEM.PUT(timer, until) END
  END PowerManagementSleep;

  PROCEDURE EnumerateHardware;
    VAR val: INTEGER;
      hasTimer, hasICIv, hasRTC, hasSPI, hasPvDsk, hasReset, hasDebugConsole: BOOLEAN;
      vRTC, ICIv, Timr, SPIf, vDsk, Rset, DbgC: INTEGER; (* hardware IDs *)
  BEGIN
    SYSTEM.PUT(hwEnumerator, 0);
    SYSTEM.GET(hwEnumerator, val);
    IF val # 1 THEN
      timer := -64; spiData := -48; spiCtrl := -44; pvDisk := 0;
      powerManagement := FALSE;
      SYSTEM.GET(timer, clockTime)
    ELSE
      vRTC := ToHardwareID("vRTC");
      ICIv := ToHardwareID("ICIv");
      Timr := ToHardwareID("Timr");
      SPIf := ToHardwareID("SPIf");
      vDsk := ToHardwareID("vDsk");
      Rset := ToHardwareID("Rset");
      DbgC := ToHardwareID("DbgC");
      SYSTEM.GET(hwEnumerator, val);
      WHILE val # 0 DO
        IF val = vRTC THEN hasRTC := TRUE
        ELSIF val = ICIv THEN hasICIv := TRUE
        ELSIF val = Timr THEN hasTimer := TRUE
        ELSIF val = SPIf THEN hasSPI := TRUE
        ELSIF val = vDsk THEN hasPvDsk := TRUE
        ELSIF val = Rset THEN hasReset := TRUE
        ELSIF val = DbgC THEN hasDebugConsole := TRUE
        END; SYSTEM.GET(hwEnumerator, val);
      END;
      IF hasTimer THEN
        SYSTEM.PUT(hwEnumerator, Timr);
        SYSTEM.GET(hwEnumerator, timer);
        SYSTEM.GET(hwEnumerator, val);
        powerManagement := (val = 1);
      ELSE
        timer := 0;
        powerManagement := FALSE;
      END;
      IF hasICIv THEN
        SYSTEM.PUT(hwEnumerator, ICIv);
        SYSTEM.GET(hwEnumerator, cacheInvalAddr);
        IF cacheInvalAddr # 0 THEN
          SYSTEM.PUT(cacheInvalAddr, 20H) (* for the TRAP handler *)
        END
      ELSE
        cacheInvalAddr := 0;
      END;
      IF hasRTC THEN
        SYSTEM.PUT(hwEnumerator, vRTC);
        SYSTEM.GET(hwEnumerator, clockTime);
        SYSTEM.GET(hwEnumerator, clock)
      ELSE
        clock := 0;
        SYSTEM.GET(timer, clockTime);
      END;
      IF hasReset THEN
        SYSTEM.PUT(hwEnumerator, Rset);
        SYSTEM.GET(hwEnumerator, softResetAddr);
        SYSTEM.GET(hwEnumerator, hardResetAddr);
        SYSTEM.GET(hwEnumerator, quitAddr)
      ELSE
        softResetAddr := 0; hardResetAddr := 0; quitAddr := 0
      END;
      IF hasDebugConsole THEN
        SYSTEM.PUT(hwEnumerator, DbgC);
        SYSTEM.GET(hwEnumerator, dbgConsole);
      ELSE
        dbgConsole := 0
      END;
      IF hasPvDsk THEN
        SYSTEM.PUT(hwEnumerator, vDsk);
        SYSTEM.GET(hwEnumerator, pvDisk);
        spiCtrl := 0; spiData := 0
      ELSIF hasSPI THEN
        SYSTEM.PUT(hwEnumerator, SPIf);
        SYSTEM.GET(hwEnumerator, spiCtrl);
        SYSTEM.GET(hwEnumerator, spiData);
        (* TODO having disk as not first device? *)
        pvDisk := 0
      ELSE
        pvDisk := 0; spiCtrl := 0; spiData := 0
      END
    END;
    (* not needed here: SYSTEM.GET(timer, clockTime); *)
  END EnumerateHardware;

(* ---------- Code Cache ---------- *)
  PROCEDURE InvalidateCodeCache*(fromAddr: INTEGER);
  BEGIN
    IF cacheInvalAddr # 0 THEN
      SYSTEM.PUT(cacheInvalAddr, fromAddr)
    END
  END InvalidateCodeCache;

(* ---------- Reset ----------*)

  PROCEDURE PerformReset(addr: INTEGER; clearLNK: BOOLEAN);
  TYPE Ptr = PROCEDURE;
  VAR resetBuffer: ARRAY 4 OF INTEGER; start: INTEGER; ptr: Ptr;
  BEGIN
    IF clearLNK THEN start := 0 ELSE start := 1 END;
    resetBuffer[0] := 04F000000H;                                 (* MOV   LNK,    0        *)
    resetBuffer[1] := 060000000H + (addr DIV 10000H MOD 10000H);  (* MOV+U R0,     HI(addr) *)
    resetBuffer[2] := 040060000H + (addr MOD 10000H);             (* IOR   R0, R0, LO(addr) *)
    resetBuffer[3] := 0C7000000H;                                 (* BC    R0               *)
    InvalidateCodeCache(SYSTEM.ADR(resetBuffer[0]));
    ptr := SYSTEM.VAL(Ptr, SYSTEM.ADR(resetBuffer[start]));
    ptr;
  END PerformReset;

  PROCEDURE SoftReset*;
  BEGIN IF softResetAddr # 0 THEN PerformReset(softResetAddr, FALSE) END
  END SoftReset;

  PROCEDURE HardReset*;
  BEGIN
    IF hardResetAddr # 0 THEN
      PerformReset(hardResetAddr, FALSE)
    ELSIF softResetAddr # 0 THEN
      PerformReset(softResetAddr, TRUE)
    END
  END HardReset;

  PROCEDURE Quit*;
  BEGIN IF quitAddr # 0 THEN PerformReset(quitAddr, FALSE) END
  END Quit;

(* ---------- Disk storage management ----------*)

  PROCEDURE SPIIdle(n: INTEGER); (*send n FFs slowly with no card selected*)
  BEGIN SYSTEM.PUT(spiCtrl, 0);
    WHILE n > 0 DO DEC(n); SYSTEM.PUT(spiData, -1);
      REPEAT UNTIL SYSTEM.BIT(spiCtrl, 0);
      SYSTEM.GET(spiData, data)
    END
  END SPIIdle;

  PROCEDURE SPI(n: INTEGER); (*send&rcv byte slowly with card selected*)
  BEGIN SYSTEM.PUT(spiCtrl, CARD0); SYSTEM.PUT(spiData, n);
    REPEAT UNTIL SYSTEM.BIT(spiCtrl, 0);
    SYSTEM.GET(spiData, data)
  END SPI;

  PROCEDURE SPICmd(n, arg: INTEGER);
    VAR i, crc: INTEGER;
  BEGIN (*send cmd*)
    REPEAT SPIIdle(1) UNTIL data = 255; (*flush while unselected*)
    REPEAT SPI(255) UNTIL data = 255; (*flush while selected*)
    IF n = 8 THEN crc := 135 ELSIF n = 0 THEN crc := 149 ELSE crc := 255 END;
    SPI(n MOD 64 + 64); (*send command*)
    FOR i := 24 TO 0 BY -8 DO SPI(ROR(arg, i)) END; (*send arg*)
    SPI(crc); i := 32;
    REPEAT SPI(255); DEC(i) UNTIL (data < 80H) OR (i = 0)
  END SPICmd;

  PROCEDURE SDShift(VAR n: INTEGER);
    VAR data: INTEGER;
  BEGIN SPICmd(58, 0);  (*CMD58 get card capacity bit*)
    SYSTEM.GET(spiData, data); SPI(-1);
    IF (data # 0) OR ~SYSTEM.BIT(spiData, 6) THEN n := n * 512 END ;  (*non-SDHC card*)
    SPI(-1); SPI(-1); SPIIdle(1)  (*flush response*)
  END SDShift;

  PROCEDURE ReadSD(src, dst: INTEGER);
    VAR i: INTEGER;
  BEGIN SDShift(src); SPICmd(17, src); ASSERT(data = 0); (*CMD17 read one block*)
    i := 0; (*wait for start data marker*)
    REPEAT SPI(-1); INC(i) UNTIL data = 254;
    SYSTEM.PUT(spiCtrl, SPIFAST + CARD0);
    FOR i := 0 TO 508 BY 4 DO
      SYSTEM.PUT(spiData, -1);
      REPEAT UNTIL SYSTEM.BIT(spiCtrl, 0);
      SYSTEM.GET(spiData, data); SYSTEM.PUT(dst, data); INC(dst, 4)
    END;
    SPI(255); SPI(255); SPIIdle(1) (*may be a checksum; deselect card*)
  END ReadSD;

  PROCEDURE WriteSD(dst, src: INTEGER);
    VAR i, n: INTEGER; x: BYTE;
  BEGIN SDShift(dst); SPICmd(24, dst); ASSERT(data = 0); (*CMD24 write one block*)
    SPI(254); (*write start data marker*)
    SYSTEM.PUT(spiCtrl, SPIFAST + CARD0);
    FOR i := 0 TO 508 BY 4 DO
      SYSTEM.GET(src, n); INC(src, 4); SYSTEM.PUT(spiData, n);
      REPEAT UNTIL SYSTEM.BIT(spiCtrl, 0)
    END;
    SPI(255); SPI(255); (*dummy checksum*) i := 0;
    REPEAT SPI(-1); INC(i); UNTIL (data MOD 32 = 5) OR (i = 10000);
    ASSERT(data MOD 32 = 5); SPIIdle(1) (*deselect card*)
  END WriteSD;

  PROCEDURE InitSlidingSecMap*(start: INTEGER);
    VAR i: INTEGER;
  BEGIN NofSectors := 0; sectorMap[0] := {0 .. 31}; sectorMap[1] := {0 .. 31};
    sectorMapStart := start; lastUsedSector := 0;
    FOR i := 2 TO mapsize DIV 32 - 1 DO sectorMap[i] := {} END
  END InitSlidingSecMap;

  PROCEDURE InitSecMap*;
  BEGIN InitSlidingSecMap(0)
  END InitSecMap;

  PROCEDURE LastUsedSector*(): INTEGER;
  BEGIN
  RETURN lastUsedSector * 29
  END LastUsedSector;

  PROCEDURE MarkSector*(sec: INTEGER);
  BEGIN sec := sec DIV 29; ASSERT(SYSTEM.H(0) = 0);
    IF sec > lastUsedSector THEN lastUsedSector := sec END;
    sec := sec - sectorMapStart * mapsize;
    IF (sec >= 0) & (sec < mapsize) THEN
      INCL(sectorMap[sec DIV 32], sec MOD 32)
    END;
    INC(NofSectors)
  END MarkSector;

  PROCEDURE FreeSector*(sec: INTEGER);
  BEGIN sec := sec DIV 29; ASSERT(SYSTEM.H(0) = 0);
    sec := sec - sectorMapStart * mapsize;
    IF (sec >= 0) & (sec < mapsize) THEN
      EXCL(sectorMap[sec DIV 32], sec MOD 32);
    END;
    DEC(NofSectors)
  END FreeSector;

  PROCEDURE AllocSector*(hint: INTEGER; VAR sec: INTEGER; VAR mapFull: BOOLEAN);
    VAR s, i: INTEGER;
  BEGIN (*find free sector, starting after hint*)
    hint := hint DIV 29; ASSERT(SYSTEM.H(0) = 0); i := 0;
    s := hint - sectorMapStart * mapsize;
    IF s < 0 THEN s := 0 END;
    REPEAT INC(s);
      IF s >= mapsize THEN INC(i); s := 1 END ;
    UNTIL (i >= 2) OR ~(s MOD 32 IN sectorMap[s DIV 32]);
    IF i >= 2 THEN
      mapFull := TRUE
    ELSE
      sec := (s + sectorMapStart * mapsize) * 29;
      MarkSector(sec)
    END
  END AllocSector;

  PROCEDURE GetSector*(src: INTEGER; VAR dst: Sector);
  BEGIN src := src DIV 29; ASSERT(SYSTEM.H(0) = 0);
    IF pvDisk # 0 THEN
      src := src + (FSoffset - 80000H) DIV 2;
      SYSTEM.PUT(pvDisk, SYSTEM.ADR(dst));
      SYSTEM.PUT(pvDisk, 080000000H + src)
    ELSE
      src := src * 2 + FSoffset;
      ReadSD(src, SYSTEM.ADR(dst)); ReadSD(src+1, SYSTEM.ADR(dst)+512)
    END
  END GetSector;
  
  PROCEDURE PutSector*(dst: INTEGER; VAR src: Sector);
  BEGIN dst := dst DIV 29; ASSERT(SYSTEM.H(0) =  0);
    IF pvDisk # 0 THEN
      dst := dst + (FSoffset - 80000H) DIV 2;
      SYSTEM.PUT(pvDisk, SYSTEM.ADR(src));
      SYSTEM.PUT(pvDisk, 0C0000000H + dst)
    ELSE
      dst := dst * 2 + FSoffset;
      WriteSD(dst, SYSTEM.ADR(src)); WriteSD(dst+1, SYSTEM.ADR(src)+512)
    END
  END PutSector;

  PROCEDURE TrimAllSectorsAfter*(sec: INTEGER);
  VAR i: INTEGER; magicSector: ARRAY 512 OF BYTE;
  BEGIN sec := sec DIV 29; ASSERT(SYSTEM.H(0) = 0);
    IF pvDisk # 0 THEN
      sec := sec + (FSoffset - 80000H) DIV 2 + 1;
      SYSTEM.PUT(pvDisk, 3FFFFFFFH);
      SYSTEM.PUT(pvDisk, 0C0000000H + sec);
    ELSE
      sec := sec * 2 + FSoffset + 2;
      magicSector[0] := ORD("!"); magicSector[504] := ORD("!");
      magicSector[1] := ORD("!"); magicSector[505] := ORD("!");
      magicSector[2] := ORD("T"); magicSector[506] := ORD("T");
      magicSector[3] := ORD("R"); magicSector[507] := ORD("R");
      magicSector[4] := ORD("I"); magicSector[508] := ORD("I");
      magicSector[5] := ORD("M"); magicSector[509] := ORD("M");
      magicSector[6] := ORD("!"); magicSector[510] := ORD("!");
      magicSector[7] := ORD("!"); magicSector[511] := ORD("!");
      FOR i := 8 TO 503 DO magicSector[i] := ORD("-") END;
      WriteSD(sec, SYSTEM.ADR(magicSector))
    END
  END TrimAllSectorsAfter;

(*-------- Miscellaneous procedures----------*)

  PROCEDURE Time*(): INTEGER;
    VAR t: INTEGER;
  BEGIN SYSTEM.GET(timer, t); RETURN t
  END Time;

  PROCEDURE AddDays(clockPart, days: INTEGER) : INTEGER;
  VAR year, month, day, monthLength : INTEGER;
  BEGIN
    year := clockPart DIV 200H;
    month := clockPart DIV 20H MOD 10H;
    day := clockPart MOD 20H + days;
    REPEAT
      IF month # 2 THEN
        monthLength := 30 + (month + month DIV 8) MOD 2
      ELSIF (year MOD 4 = 0) & ((year MOD 100 # 0) OR (year MOD 400 = 0)) THEN
        monthLength := 29
      ELSE
        monthLength := 28
      END;
      IF day > monthLength THEN
        INC(month); DEC(day, monthLength);
        IF month = 13 THEN INC(year); month := 1 END;
        monthLength := 28 (* will recalculate in next loop run, if required *)
      END
    UNTIL day <= monthLength;
  RETURN (year * 10H + month) * 20H + day
  END AddDays;
  
  PROCEDURE UpdateClock;
  VAR value, rest: INTEGER;
  BEGIN
    IF clock = 0 THEN
      value := 0 (* avoid ticking if not set *)
    ELSE
      value := (Time() - clockTime) DIV 1000;
      clockTime := clockTime + value * 1000;
      value := clock MOD 40H + value
    END;
    IF value < 60 THEN
      clock := clock DIV 40H * 40H + value
    ELSE
      rest := value MOD 60;
      value := value DIV 60 + clock DIV 40H MOD 40H;
      IF value < 60 THEN
        clock := clock DIV 1000H * 1000H + value * 40H + rest
      ELSE
        rest := (value MOD 60) * 40H + rest;
        value := value DIV 60 + clock DIV 1000H MOD 32;
        IF value < 24 THEN
          clock := clock DIV 20000H * 20000H + value * 1000H + rest
        ELSE
          clock := AddDays(clock DIV 20000H, value DIV 24) * 20000H + (value MOD 24) * 1000H + rest;
        END
      END
    END
  END UpdateClock;
  
  PROCEDURE Clock*(): INTEGER;
  BEGIN UpdateClock; RETURN clock
  END Clock;

  PROCEDURE SetClock*(dt: INTEGER);
  BEGIN clock := dt; clockTime := Time()
  END SetClock;

  PROCEDURE AddWeakReference*(ref: WeakReference);
  BEGIN
    ref.nextRef := weakRefRoot;
    weakRefRoot := ref;
  END AddWeakReference;

  PROCEDURE RestoreWeakReferences*; (*after mark phase of garbage collection*)
    VAR r: WeakReference;

    PROCEDURE unmarked(r: WeakReference): BOOLEAN;
      VAR m: INTEGER;
    BEGIN
      IF (r = NIL) OR (r.target = 0) THEN m := -1 ELSE SYSTEM.GET(r.target-4, m) END;
      RETURN m = 0
    END unmarked;

  BEGIN
    WHILE unmarked(weakRefRoot) DO weakRefRoot.target := 0; weakRefRoot := weakRefRoot.nextRef END;
    r := weakRefRoot; (* r is marked or NIL *)
    WHILE r # NIL DO
      WHILE unmarked(r.nextRef) DO r.nextRef.target := 0; r.nextRef := r.nextRef.nextRef END;
      r := r.nextRef
    END
  END RestoreWeakReferences;

  PROCEDURE Install*(Padr, at: INTEGER);
  BEGIN SYSTEM.PUT(at, 0E7000000H + (Padr - at) DIV 4 -1)
    ;InvalidateCodeCache(at)
  END Install;

  PROCEDURE Trap(VAR a: INTEGER; b: INTEGER);
    VAR u, v, w: INTEGER;
  BEGIN u := SYSTEM.REG(15); SYSTEM.GET(u - 4, v); w := v DIV 10H MOD 10H; (*trap number*)
    IF w = 0 THEN New(a, b)
    ELSE (*stop*) LED(w + 192); REPEAT UNTIL FALSE
    END
  END Trap;

  PROCEDURE ForceFree*(adr: INTEGER);
    VAR tag, size: INTEGER;
  BEGIN
    SYSTEM.GET(adr, tag); SYSTEM.GET(tag, size);
    ASSERT((size > 0) & (size MOD 256 = 0)); DEC(allocated, size);
    SYSTEM.PUT(adr, size); SYSTEM.PUT(adr + 4, -1); SYSTEM.PUT(adr + 8, list0); list0 := adr;
  END ForceFree;

  PROCEDURE Init*;
  BEGIN Install(SYSTEM.ADR(Trap), 20H);  (*install temporary trap*)
    SYSTEM.GET(12, MemLim); SYSTEM.GET(24, heapOrg);
    stackOrg := heapOrg; stackSize := 8000H; heapLim := MemLim;
    weakRefRoot := NIL;
    list1 := 0; list2 := 0; list3 := 0; list0 := heapOrg;
    clockTime := Time();
    SYSTEM.PUT(list0, heapLim - heapOrg); SYSTEM.PUT(list0+4, -1); SYSTEM.PUT(list0+8, 0);
    allocated := 0; clock := 0; InitSecMap
    ;EnumerateHardware
  END Init;

END Kernel.
