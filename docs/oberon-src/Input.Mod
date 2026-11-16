MODULE Input; (*NW 5.10.86 / 15.11.90 Ceres-2; PDR 21.4.12 / NW 15.5.2013 Ceres-4*)
  IMPORT SYSTEM;

  VAR kbdCode: BYTE; (*last keyboard code read*)
    msAdr, kbdAdr: INTEGER;
    pvKbd: BOOLEAN;
    Recd, Up, Shift, Ctrl, Ext: BOOLEAN;
    KTabAdr: INTEGER;  (*keyboard code translation table*)
    MW, MH, MX, MY: INTEGER; (*mouse limits and coords*)
    MK: SET; (*mouse keys*)
    spacePending: BOOLEAN;
    keyPending: CHAR;
    overrideKeys, overrideInterclick: SET;
    overrideCount: INTEGER;

(*FIFO implemented in hardware, because every read must be handled,
  including tracking the state of the Shift and Ctrl keys*)
  
  PROCEDURE Peek();
    VAR pvKbdCode: INTEGER;
  BEGIN
    WHILE ~Recd & SYSTEM.BIT(msAdr, 28) DO
     IF pvKbd THEN
      SYSTEM.GET(kbdAdr, pvKbdCode);
      kbdCode := pvKbdCode DIV 1000000H;
      IF kbdCode # 0H THEN Recd := TRUE END;
     ELSE
      SYSTEM.GET(kbdAdr, kbdCode);
      IF kbdCode = 0F0H THEN Up := TRUE
      ELSIF kbdCode = 0E0H THEN Ext := TRUE
      ELSE
        IF (kbdCode = 12H) OR (kbdCode = 59H) THEN (*shift*) Shift := ~Up
        ELSIF kbdCode = 14H THEN (*ctrl*) Ctrl := ~Up
        ELSIF ~Up THEN Recd := TRUE (*real key going down*)
        END ;
        Up := FALSE; Ext := FALSE
      END
     END
    END;
  END Peek;

  PROCEDURE Available*(): INTEGER;
  BEGIN IF (~spacePending) & (keyPending = 0X) THEN Peek() END;
    RETURN ORD(Recd OR spacePending OR (keyPending # 0X))
  END Available;

  PROCEDURE InjectChar*(injectSpace: BOOLEAN; injectChar: CHAR);
  BEGIN spacePending := injectSpace; keyPending := injectChar
  END InjectChar;

  PROCEDURE OverrideMouseKeys*(keys, interclick: SET);
  BEGIN
    overrideKeys := keys; overrideInterclick := interclick; overrideCount := 0
  END OverrideMouseKeys;

  PROCEDURE Read*(VAR ch: CHAR);
  BEGIN
    IF spacePending THEN ch := 20X; spacePending := FALSE
    ELSIF keyPending # 0X THEN ch := keyPending; keyPending := 0X ELSE
    WHILE ~Recd DO Peek() END ;
    IF Shift OR Ctrl THEN INC(kbdCode, 80H) END; (*ctrl implies shift*)
  (* ch := kbdTab[kbdCode]; *)
    SYSTEM.GET(KTabAdr + kbdCode, ch);
    IF Ctrl THEN ch := CHR(ORD(ch) MOD 20H) END;
    IF pvKbd THEN ch := CHR(kbdCode) END;
    Recd := FALSE
    END
  END Read;

  PROCEDURE Mouse*(VAR keys: SET; VAR x, y: INTEGER);
    VAR w: INTEGER;
  BEGIN SYSTEM.GET(msAdr, w);
    keys := SYSTEM.VAL(SET, w DIV 1000000H MOD 8);
    IF (keys # {}) & (overrideKeys # {}) THEN
      keys := overrideKeys;
      IF overrideInterclick = {} THEN overrideCount := 10 ELSE overrideCount := 1 END
    ELSIF (keys = {}) & (overrideCount >= 1) & (overrideCount < 10) THEN
      keys := overrideInterclick; INC(overrideCount)
    ELSIF (keys = {}) & (overrideCount = 10) THEN
      overrideKeys := {}; overrideInterclick := {}; overrideCount := 0
    END;
    x := w MOD 1000H; y := (w DIV 1000H) MOD 1000H;
    IF y >= MH THEN y := MH-1 END
  END Mouse;

  PROCEDURE SetMouseLimits*(w, h: INTEGER);
  BEGIN MW := w; MH := h
  END SetMouseLimits;

  PROCEDURE Init*;
  BEGIN Up := FALSE; Shift := FALSE; Ctrl := FALSE; Recd := FALSE;
    spacePending := FALSE; keyPending := 0X;
    KTabAdr := SYSTEM.ADR($
      00 00 00 00 00 1A 00 00  00 00 00 00 00 09 60 00
      00 00 00 00 00 71 31 00  00 00 7A 73 61 77 32 00
      00 63 78 64 65 34 33 00  00 20 76 66 74 72 35 00
      00 6E 62 68 67 79 36 00  00 00 6D 6A 75 37 38 00
      00 2C 6B 69 6F 30 39 00  00 2E 2F 6C 3B 70 2D 00
      00 00 27 00 5B 3D 00 00  00 00 0D 5D 00 5C 00 00
      00 00 00 00 00 00 08 00  00 00 00 11 00 00 00 00
      00 7F 14 00 12 13 1B 00  00 00 00 00 00 00 00 00
      00 00 00 00 00 00 00 00  00 00 00 00 00 09 7E 00
      00 00 00 00 00 51 21 00  00 00 5A 53 41 57 40 00
      00 43 58 44 45 24 23 00  00 20 56 46 54 52 25 00
      00 4E 42 48 47 59 5E 00  00 00 4D 4A 55 26 2A 00
      00 3C 4B 49 4F 29 28 00  00 3E 3F 4C 3A 50 5F 00
      00 00 22 00 7B 2B 00 00  00 00 0D 7D 00 7C 00 00
      00 00 00 00 00 00 08 00  00 00 00 11 00 00 00 00
      00 7F 14 00 12 13 1B 00  00 00 00 00 00 00 00 00$)
  END Init;

  PROCEDURE EnumerateHardware;
      VAR val, MsKb: INTEGER;
  BEGIN
    MsKb := 4D734B62H;
    SYSTEM.PUT(-4, MsKb);
    SYSTEM.GET(-4, msAdr);
    SYSTEM.GET(-4, kbdAdr);
    SYSTEM.GET(-4, val);
    pvKbd := val = 1;
    IF kbdAdr = 0 THEN msAdr := -40; kbdAdr := -36 END
  END EnumerateHardware;

BEGIN EnumerateHardware; Init
END Input.
