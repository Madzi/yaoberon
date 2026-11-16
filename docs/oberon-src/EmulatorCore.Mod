MODULE EmulatorCore;

  IMPORT SYSTEM, Kernel, Files, Texts, Oberon, EmulatorCPU;

  TYPE Emulator* = POINTER TO EmulatorDesc;

    DisplayNotify* = PROCEDURE(e: Emulator; minX, maxX, minY, maxY: INTEGER);

    EmulatorDesc* = RECORD (EmulatorCPU.StateDesc)
      waitMillis, clipboardBufferPos, paravirtPointer: INTEGER;
      disk: Files.File;
      notify: DisplayNotify;
      keyBuffer: ARRAY 16 OF CHAR;
      keyBufferStart, keyBufferEnd, mouseStatus: INTEGER;
      useSerialPort, stopped*, sector0missing: BOOLEAN;
      hwEnumBuffer: ARRAY 32 OF INTEGER;
      hwEnumAddr, hwEnumCount: INTEGER;
    END;

    EmulatorContinue = POINTER TO EmulatorContinueDesc;
    EmulatorContinueDesc = RECORD
      next: EmulatorContinue;
      e: Emulator;
      timestamp: INTEGER
    END;

  VAR T1, T2: Oberon.Task;
    EC: EmulatorContinue;
    CW: Texts.Writer;

  PROCEDURE MemReadIO(e: Emulator; address: INTEGER; VAR val: INTEGER);
    VAR T: Texts.Text;
      R: Texts.Reader;
      ch: CHAR;
      beg, end, time: INTEGER;
  BEGIN
    IF address = EmulatorCPU.IoStart THEN
      val := Kernel.Time()
    ELSIF e.useSerialPort & (address = EmulatorCPU.IoStart + 8) THEN
      SYSTEM.GET(-56, val)
    ELSIF e.useSerialPort & (address = EmulatorCPU.IoStart + 12) THEN
      SYSTEM.GET(-48, val)
    ELSIF address = EmulatorCPU.IoStart + 24 THEN
      val := e.mouseStatus;
      IF e.keyBufferStart # e.keyBufferEnd THEN val := val + 10000000H END;
    ELSIF address = EmulatorCPU.IoStart + 28 THEN
      IF e.keyBufferStart = e.keyBufferEnd THEN
        val := 0
      ELSE
        val := ORD(e.keyBuffer[e.keyBufferStart]) * 1000000H;
        e.keyBufferStart := (e.keyBufferStart + 1) MOD 16
      END
    ELSIF address = EmulatorCPU.IoStart + 40 THEN
      e.clipboardBufferPos := 0;
      Oberon.GetSelection(T, beg, end, time);
      IF time >= 0 THEN
        val := end - beg;
      ELSE
        val := 0
      END
    ELSIF address = EmulatorCPU.IoStart + 44 THEN
      Oberon.GetSelection(T, beg, end, time);
      IF (time >= 0) & (e.clipboardBufferPos >= 0) & (e.clipboardBufferPos < end - beg) THEN
        Texts.OpenReader(R, T, beg + e.clipboardBufferPos);
        INC(e.clipboardBufferPos);
        Texts.Read(R, ch);
        val := ORD(ch)
      ELSE
        val := 0
      END
    ELSIF (address = EmulatorCPU.IoStart + 60) & (e.hwEnumAddr < e.hwEnumCount) THEN
        val := e.hwEnumBuffer[e.hwEnumAddr];
        INC(e.hwEnumAddr)
    ELSE
      val := 0
    END
  END MemReadIO;

  PROCEDURE MemWriteIO(e: Emulator; address, val: INTEGER);
    VAR sector, addr, i, v: INTEGER;
      R: Files.Rider;
  BEGIN
    IF address = EmulatorCPU.IoStart THEN
      IF e.waitMillis = -1 THEN
        e.waitMillis := 0
      ELSE
        e.waitMillis := val
      END
    ELSIF address = EmulatorCPU.IoStart + 4 THEN
      LED(val)
    ELSIF e.useSerialPort & (address = EmulatorCPU.IoStart + 8) THEN
      SYSTEM.PUT(-56, val)
    ELSIF e.useSerialPort & (address = EmulatorCPU.IoStart + 12) THEN
      SYSTEM.PUT(-48, val)
    ELSIF address = EmulatorCPU.IoStart + 36 THEN
      IF val >= 0 THEN
        e.paravirtPointer := val
      ELSE
        sector := val MOD 40000000H;
        addr := SYSTEM.ADR(e.mem.ram) + e.paravirtPointer;
        IF e.sector0missing THEN DEC(sector) END;
        Files.Set(R, e.disk, sector * 1024);
        IF val DIV 40000000H MOD 2 = 0 THEN
          FOR i := 0 TO 255 DO
            Files.ReadInt(R, v); SYSTEM.PUT(addr, v); INC(addr, 4)
          END
        ELSIF e.paravirtPointer = 3FFFFFFFH THEN
          (* ignore trim operation *)
        ELSE
          FOR i := 0 TO 255 DO
            SYSTEM.GET(addr, v); INC(addr, 4); Files.WriteInt(R, v)
          END
        END
      END
    ELSIF address = EmulatorCPU.IoStart + 40 THEN
      Texts.WriteString(CW, "Clipboard Copy: ");
      e.clipboardBufferPos := -val;
    ELSIF address = EmulatorCPU.IoStart + 44 THEN
      IF e.clipboardBufferPos < 0 THEN
        Texts.Write(CW, CHR(val));
        INC(e.clipboardBufferPos);
        IF e.clipboardBufferPos = 0 THEN
          Texts.WriteLn(CW);
          Texts.Append(Oberon.Log, CW.buf)
        END
      END
    ELSIF address = EmulatorCPU.IoStart + 60 THEN
      e.hwEnumAddr := 0; e.hwEnumCount := 0;
      IF val = 0 THEN
        e.hwEnumBuffer[e.hwEnumCount] := 1; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := Kernel.ToHardwareID("mVid"); INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := Kernel.ToHardwareID("Timr"); INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := Kernel.ToHardwareID("LEDs"); INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := Kernel.ToHardwareID("MsKb"); INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := Kernel.ToHardwareID("vClp"); INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := Kernel.ToHardwareID("vDsk"); INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := Kernel.ToHardwareID("vRTC"); INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := Kernel.ToHardwareID("Rset"); INC(e.hwEnumCount);
        IF e.useSerialPort THEN
          e.hwEnumBuffer[e.hwEnumCount] := Kernel.ToHardwareID(""); INC(e.hwEnumCount)
        END;
      ELSIF val = Kernel.ToHardwareID("mVid") THEN
        e.hwEnumBuffer[e.hwEnumCount] := 1; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := 0; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := e.width; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := e.height; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := 128; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := EmulatorCPU.DisplayStart; INC(e.hwEnumCount);
      ELSIF val = Kernel.ToHardwareID("Rset") THEN
         e.hwEnumBuffer[e.hwEnumCount] := EmulatorCPU.RomStart; INC(e.hwEnumCount);
      ELSIF (val = Kernel.ToHardwareID("SPrt")) & e.useSerialPort THEN
        e.hwEnumBuffer[e.hwEnumCount] := 1; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := -52; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := -56; INC(e.hwEnumCount);
      ELSIF val = Kernel.ToHardwareID("Timr") THEN
        e.hwEnumBuffer[e.hwEnumCount] := -64; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := 1; INC(e.hwEnumCount);
      ELSIF val = Kernel.ToHardwareID("LEDs") THEN
        e.hwEnumBuffer[e.hwEnumCount] := 8; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := -60; INC(e.hwEnumCount);
      ELSIF val = Kernel.ToHardwareID("MsKb") THEN
        e.hwEnumBuffer[e.hwEnumCount] := -40; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := -36; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := 1; INC(e.hwEnumCount);
      ELSIF val = Kernel.ToHardwareID("vClp") THEN
        e.hwEnumBuffer[e.hwEnumCount] := -24; INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := -20; INC(e.hwEnumCount);
      ELSIF val = Kernel.ToHardwareID("vRTC") THEN
        e.hwEnumBuffer[e.hwEnumCount] := Kernel.Time(); INC(e.hwEnumCount);
        e.hwEnumBuffer[e.hwEnumCount] := Kernel.Clock(); INC(e.hwEnumCount);
      ELSIF val = Kernel.ToHardwareID("vDsk") THEN
        e.hwEnumBuffer[e.hwEnumCount] := -28; INC(e.hwEnumCount);
      END
    ELSE
      val := 0
    END
  END MemWriteIO;

  PROCEDURE HandleIO(s: EmulatorCPU.State; write: BOOLEAN; address: INTEGER; VAR val: INTEGER);
  BEGIN
    IF write THEN
      MemWriteIO(s(Emulator), address, val)
    ELSE
      MemReadIO(s(Emulator), address, val)
    END
  END HandleIO;

  PROCEDURE EnableTasks;
  BEGIN
    Oberon.Remove(T1); Oberon.Remove(T2);
    IF (EC # NIL) & (EC.timestamp < Kernel.Time()+900) THEN
      Oberon.Install(T1)
    ELSIF EC # NIL THEN
      Oberon.Install(T2)
    END;
  END EnableTasks;

  PROCEDURE RemoveContinue(e: Emulator);
    VAR ec: EmulatorContinue;
  BEGIN
    WHILE (EC # NIL) & (EC.e = e) DO EC := EC.next END;
    ec := EC;
    WHILE (ec # NIL) DO
      WHILE (ec.next # NIL) & (ec.next.e = e) DO ec.next := ec.next.next END;
      ec := ec.next
    END
  END RemoveContinue;

  PROCEDURE Run*(e: Emulator);
  VAR now, i, minX, minY, maxX, maxY: INTEGER;
    ec, ec2: EmulatorContinue;
  BEGIN
    IF ~e.stopped THEN
      RemoveContinue(e);
      now := Kernel.Time(); i := 0;
      WHILE (i < 200000) & (e.waitMillis < now) DO
        EmulatorCPU.Step(e); INC(i)
      END;
      IF EmulatorCPU.NotifyDisplayNeeded(e, minX, maxX, minY, maxY) THEN
        e.notify(e, minX, maxX, minY, maxY)
      END;
      NEW(ec);
      ec.e := e;
      ec.timestamp := e.waitMillis;
      now := Kernel.Time();
      IF (ec.timestamp < now + 10) THEN ec.timestamp := now + 10 END;
      IF (EC = NIL) OR (EC.timestamp > ec.timestamp) THEN
        ec.next := EC; EC := ec
      ELSE
        ec2 := EC;
        WHILE (ec2.next # NIL) & (ec2.next.timestamp <= ec.timestamp) DO ec2 := ec2.next END;
        ec.next := ec2.next;
        ec2.next := ec
      END;
      EnableTasks
    END
  END Run;

  PROCEDURE Reset*(e: Emulator; cold: BOOLEAN);
  BEGIN
    IF (e # NIL) & (~e.stopped) THEN
      EmulatorCPU.Reset(e, cold);
      IF cold THEN
        EmulatorCPU.WriteWord(e, EmulatorCPU.DisplayStart, 53697A65H); (* magic value "Size" *)
        EmulatorCPU.WriteWord(e, EmulatorCPU.DisplayStart + 4, e.width);
        EmulatorCPU.WriteWord(e, EmulatorCPU.DisplayStart + 8, e.height);
      END;
      Run(e)
    END
  END Reset;


  PROCEDURE Init*(disk: Files.File; notify: DisplayNotify; width, height: INTEGER; useSerialPort: BOOLEAN): Emulator;
  VAR e: Emulator; i: INTEGER; R: Files.Rider;
  BEGIN
    width := width DIV 32 * 32;
    NEW(e);
    IF e # NIL THEN EmulatorCPU.Init(e, width, height, HandleIO) END;
    IF (e = NIL) OR (e.mem = NIL) THEN
      Texts.WriteString(CW, "Not enough memory");
      Texts.WriteLn(CW);
      Texts.Append(Oberon.Log, CW.buf)
    ELSE
      e.disk := disk;
      Files.Set(R, disk, 0);
      Files.ReadInt(R, i);
      IF i = 9B1EA38DH THEN
        Files.Set(R, disk, 1024);
        Files.ReadInt(R, i);
        Texts.WriteHex(CW, i);
        IF i # 9B1EA38DH THEN e.sector0missing := TRUE END
      END;
      e.notify := notify;
      e.useSerialPort := useSerialPort
    END
  RETURN e
  END Init;

  PROCEDURE ResetWaitMillis*(e: Emulator);
  BEGIN e.waitMillis := -1
  END ResetWaitMillis;

  PROCEDURE SetMouse*(e: Emulator; x, y: INTEGER; keys: SET);
  BEGIN
    e.mouseStatus := x + y * 1000H + SYSTEM.VAL(INTEGER, keys) * 1000000H;
    ResetWaitMillis(e); Run(e);
  END SetMouse;

  PROCEDURE AddKey*(e: Emulator; ch: CHAR);
  BEGIN
    IF (e.keyBufferEnd + 1) MOD 16 # e.keyBufferStart THEN
      e.keyBuffer[e.keyBufferEnd] := ch;
      e.keyBufferEnd := (e.keyBufferEnd + 1) MOD 16;
      ResetWaitMillis(e); Run(e);
    END
  END AddKey;

  PROCEDURE Stop*(e: Emulator);
  BEGIN
    IF (e # NIL) & (~e.stopped) THEN
      e.stopped := TRUE;
      RemoveContinue(e);
      EnableTasks;
      e.notify(e, 0, 0, 0, 0)
    END
  END Stop;

  PROCEDURE ContinueRun;
    VAR now: INTEGER; ec, ec2: EmulatorContinue;
  BEGIN
    Oberon.Remove(T1); Oberon.Remove(T2);
    now := Kernel.Time();
    ec := EC; ec2 := NIL;
    WHILE (EC # NIL) & (EC.timestamp < now) DO ec2 := EC; EC := EC.next END;
    IF (ec2 # NIL) & (ec2.next = EC) THEN ec2.next := NIL END;
    WHILE ec # NIL DO
      Run(ec.e);
      ec := ec.next
    END;
    IF EC # NIL THEN EnableTasks END;
  END ContinueRun;

BEGIN Texts.OpenWriter(CW);
  T1 := Oberon.NewTask(ContinueRun, 10); T2 := Oberon.NewTask(ContinueRun, 1000);
END EmulatorCore.
