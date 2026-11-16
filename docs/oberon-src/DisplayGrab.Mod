MODULE DisplayGrab;
  IMPORT SYSTEM, Display;

  PROCEDURE GrabDot*(x, y: INTEGER) : INTEGER;
    VAR a, col: INTEGER; s: SET;
  BEGIN
    a := Display.Base + (x DIV (32 DIV Display.Depth)) * 4 + y * Display.Span;
    IF Display.Depth = 1 THEN
      col := 1; SYSTEM.GET(a, s); IF (s * {x MOD 32} = {}) THEN col := 0 END
    ELSIF Display.Depth = 4 THEN
      SYSTEM.GET(a, col); col := ASR(col, (x MOD 8) * 4) MOD 10H;
    ELSE
      col := 0
    END
    RETURN col
  END GrabDot;

  PROCEDURE GrabPatternRow*(x, y, w: INTEGER): INTEGER;
    VAR row, a, aL, aR: INTEGER; pix, mask: SET;
  BEGIN
    ASSERT((w > 0) & (w <= 32));
    aL := Display.Base + y*Display.Span; aR := ((x+w-1) DIV 32)*4 + aL; aL := (x DIV 32)*4 + aL;
    IF Display.Depth # 1 THEN
      pix := {};
      FOR row := 0 TO w - 1 DO
        IF GrabDot(x+row, y) # 0 THEN INCL(pix, row) END
      END;
      row := SYSTEM.VAL(INTEGER, pix);
    ELSIF aR = aL THEN
      mask := {(x MOD 32) .. ((x+w-1) MOD 32)};
      SYSTEM.GET(aL, pix);
      row := ASR(SYSTEM.VAL(INTEGER, pix * mask), x MOD 32)
    ELSIF aR = aL + 4 THEN
      mask := {(x MOD 32) .. 31};
      SYSTEM.GET(aL, pix);
      row := ASR(SYSTEM.VAL(INTEGER, pix * mask), x MOD 32);
      mask := {0 .. 31 - x MOD 32};
      row := SYSTEM.VAL(INTEGER, SYSTEM.VAL(SET, row) * mask);
      mask := {0 .. ((x+w-1) MOD 32)};
      SYSTEM.GET(aR, pix);
      row := row + LSL(SYSTEM.VAL(INTEGER, pix * mask), 32 - x MOD 32)
    ELSE ASSERT(FALSE)
    END;
    RETURN row
  END GrabPatternRow;

  PROCEDURE CopyColorPattern*(patadr, x, y: INTEGER);
    VAR a, a0, pwd, xx, yy, i: INTEGER;
      w, h, pbt: BYTE; pix, mask: SET;
  BEGIN
    SYSTEM.GET(patadr, w); SYSTEM.GET(patadr+1, h); INC(patadr, 2); xx := x; yy := y;
    a := Display.Base + (x DIV 8)*4 + y*Display.Span; x := x MOD 8; mask := SYSTEM.VAL(SET, ASR(7FFFFFFFH, 31-(x*4)));
    a0 := a;
    WHILE a0 < a + h * Display.Span DO
      (*build pattern line; w <= 8*)
      SYSTEM.GET(patadr, pbt); INC(patadr); pwd := pbt;
      IF w > 2 THEN SYSTEM.GET(patadr, pbt); INC(patadr); pwd := pbt*100H + pwd;
        IF w > 4 THEN SYSTEM.GET(patadr, pbt); INC(patadr); pwd := pbt*10000H + pwd;
          IF w > 6 THEN SYSTEM.GET(patadr, pbt); INC(patadr); pwd := pbt*1000000H + pwd END
        END
      END;
      IF Display.Depth = 4 THEN
        SYSTEM.GET(a0, pix);
        SYSTEM.PUT(a0, SYSTEM.VAL(SET, LSL(pwd, x * 4)) + pix);
        IF x + w > 8 THEN (*spill over*)
          SYSTEM.GET(a0+4, pix);
          SYSTEM.PUT(a0+4, SYSTEM.VAL(SET, ASR(pwd, -(x * 4))) * mask + pix)
        END;
      ELSE
        FOR i := xx TO xx + w - 1 DO
          Display.Dot(pwd DIV LSL(1, (i - xx) * 4) MOD 10H, i, yy, Display.replace)
        END;
        INC(yy);
      END;
      INC(a0, Display.Span)
    END
  END CopyColorPattern;

  PROCEDURE GetPalette*(col: INTEGER; VAR val: INTEGER);
    VAR success : BOOLEAN;
  BEGIN
    success := Display.GetPalette(col, val);
    IF ~success THEN
      IF col = 0 THEN val := 0 ELSE val := 0FFFFFFH END
    END
  END GetPalette;

  PROCEDURE SetPalette*(col, val: INTEGER);
  BEGIN Display.SetPalette(col, val);
  END SetPalette;

BEGIN
END DisplayGrab.
