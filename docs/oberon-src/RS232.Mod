MODULE RS232;   (*NW 3.1.2012*)
  IMPORT SYSTEM;

  VAR data, stat, pmgmt: INTEGER;

  PROCEDURE Idle;
  BEGIN
    IF pmgmt # 0 THEN SYSTEM.PUT(pmgmt, 7FFFFFFFH) END
  END Idle;

  PROCEDURE Available*() : BOOLEAN;
  RETURN SYSTEM.BIT(stat, 0)
  END Available;

  PROCEDURE Send*(x: INTEGER);
  BEGIN
    WHILE ~SYSTEM.BIT(stat, 1) DO Idle END;
    SYSTEM.PUT(data, x)
  END Send;
  
  PROCEDURE Rec*(VAR x: INTEGER);
  BEGIN
    WHILE ~Available() DO Idle END;
    SYSTEM.GET(data, x)
  END Rec;

  PROCEDURE SendIntRaw*(x: INTEGER);
    VAR i: INTEGER;
  BEGIN i := 4;
    REPEAT i := i-1; Send(x);  x := ROR(x, 8) UNTIL i = 0
  END SendIntRaw;

  PROCEDURE SendInt*(x: INTEGER);
  BEGIN Send(1); SendIntRaw(x)
  END SendInt;

  PROCEDURE SendHex*(x: INTEGER);
    VAR i: INTEGER;
  BEGIN Send(2); i := 4;
    REPEAT i := i-1; Send(x);  x := ROR(x, 8) UNTIL i = 0
  END SendHex;

  PROCEDURE SendReal*(x: REAL);
    VAR i, u: INTEGER;
  BEGIN Send(3); u := ORD(x); i := 4;
    REPEAT i := i-1; Send(u);  u := ROR(u, 8) UNTIL i = 0
  END SendReal;

  PROCEDURE SendStrRaw*(x: ARRAY OF CHAR);
    VAR i, k: INTEGER;
  BEGIN i := 0;
    REPEAT k := ORD(x[i]); Send(k); INC(i) UNTIL k = 0
  END SendStrRaw;

  PROCEDURE SendStr*(x: ARRAY OF CHAR);
  BEGIN Send(4); SendStrRaw(x)
  END SendStr;

  PROCEDURE RecInt*(VAR x: INTEGER);
    VAR i, x0, y: INTEGER;
  BEGIN i := 4; x0 := 0;
    REPEAT i := i-1; Rec(y); x0 := ROR(x0+y, 8) UNTIL i = 0;
    x := x0
  END RecInt;

  PROCEDURE RecReal*(VAR x: REAL);
    VAR i, x0, y: INTEGER;
  BEGIN i := 4; x0 := 0;
    REPEAT i := i-1; Rec(y); x0 := ROR(x0+y, 8) UNTIL i = 0;
    x := SYSTEM.VAL(REAL, x0)
  END RecReal;

  PROCEDURE RecStr*(VAR x: ARRAY OF CHAR);
    VAR i, k: INTEGER;
  BEGIN i := 0;
    REPEAT Rec(k); x[i] := CHR(k); INC(i) UNTIL k = 0
  END RecStr;

  PROCEDURE Line*;
  BEGIN Send(6)
  END Line;
  
  PROCEDURE End*;
  BEGIN Send(7)
  END End;

  PROCEDURE InitIO;
    VAR count: INTEGER;
  BEGIN
    SYSTEM.PUT(-4, 53507274H); (* SPrt *)
    SYSTEM.GET(-4, count);
    IF count > 0 THEN
      SYSTEM.GET(-4, stat);
      SYSTEM.GET(-4, data)
    ELSE
      data := -56; stat := -52
    END;
    SYSTEM.PUT(-4, 54696D72H); (* Timr *)
    SYSTEM.GET(-4, pmgmt);
    SYSTEM.GET(-4, count);
    IF count # 1 THEN pmgmt := 0 END
  END InitIO;

BEGIN InitIO
END RS232.
