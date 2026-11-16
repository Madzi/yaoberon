MODULE Edit; (*JG 2.11.90 / NW 28.11.2015*)
  IMPORT Files, Fonts, Texts, Display, Viewers, Oberon, MenuViewers, TextFrames;

  CONST CR = 0DX; maxlen = 32;
    StandardMenu = "System.Close System.Copy System.Grow Edit.Search Edit.Store";

  TYPE Line = POINTER TO LineDesc;
    LineDesc = RECORD
      p, n: Line;
      b, e, h: INTEGER;
      c: BOOLEAN
    END;

  VAR W: Texts.Writer;
    time: LONGINT;
    M: INTEGER;
    pat: ARRAY maxlen OF CHAR;
    d: ARRAY 256 OF INTEGER;

  PROCEDURE Open*;
    VAR T: Texts.Text;
    S: Texts.Scanner;
    V: Viewers.Viewer;
    X, Y: INTEGER;
    beg, end, time: LONGINT;
  BEGIN Texts.OpenScanner(S, Oberon.Par.text, Oberon.Par.pos); Texts.Scan(S);
    IF (S.class = Texts.Char) & (S.c = "^") OR (S.line # 0) THEN
      Oberon.GetSelection(T, beg, end, time);
      IF time >= 0 THEN Texts.OpenScanner(S, T, beg); Texts.Scan(S) END
    END;
    IF S.class = Texts.Name THEN
      Oberon.AllocateUserViewer(Oberon.Par.vwr.X, X, Y);
      V := MenuViewers.New(
        TextFrames.NewMenu(S.s, StandardMenu),
        TextFrames.NewText(TextFrames.Text(S.s), 0),
        TextFrames.menuH, X, Y)
    END
  END Open;

  PROCEDURE Store*;
    VAR V: Viewers.Viewer;
      Text: TextFrames.Frame;
      T: Texts.Text;
      S: Texts.Scanner;
      f: Files.File; R: Files.Rider;
      beg, end, time, len: LONGINT;

    PROCEDURE Backup (VAR name: ARRAY OF CHAR);
      VAR res, i: INTEGER; bak: ARRAY 32 OF CHAR;
    BEGIN i := 0;
      WHILE name[i] # 0X DO bak[i] := name[i]; INC(i) END;
      bak[i] := "."; bak[i+1] := "B"; bak[i+2] := "a"; bak[i+3] := "k"; bak[i+4] := 0X;
      Files.Rename(name, bak, res)
    END Backup;

  BEGIN Texts.WriteString(W, "Edit.Store ");
    IF Oberon.Par.frame = Oberon.Par.vwr.dsc THEN
      V := Oberon.Par.vwr; Texts.OpenScanner(S, V.dsc(TextFrames.Frame).text, 0)
    ELSE V := Oberon.MarkedViewer(); Texts.OpenScanner(S, Oberon.Par.text, Oberon.Par.pos)
    END;
    Texts.Scan(S);
    IF (S.class = Texts.Char) & (S.c = "^") THEN
      Oberon.GetSelection(T, beg, end, time);
      IF time >= 0 THEN Texts.OpenScanner(S, T, beg); Texts.Scan(S) END
    END;
    IF (S.class = Texts.Name) & (V.dsc # NIL) & (V.dsc.next IS TextFrames.Frame) THEN
      Text := V.dsc.next(TextFrames.Frame);
      Texts.WriteString(W, S.s); Texts.WriteInt(W, Text.text.len, 8);
      Texts.WriteLn(W); Texts.Append(Oberon.Log, W.buf);
      Backup(S.s); Texts.Close(Text.text, S.s)
    END
  END Store;

  PROCEDURE CopyLooks*;
    VAR T: Texts.Text;
      F: TextFrames.Frame;
      v: Viewers.Viewer;
      beg, end, time: LONGINT;
      fnt: Fonts.Font; col, voff: INTEGER;
  BEGIN Oberon.GetSelection(T, beg, end, time);
    IF time >= 0 THEN
      v := Oberon.FocusViewer;
      IF (v # NIL) & (v.dsc # NIL) & (v.dsc.next IS TextFrames.Frame) THEN
        F := v.dsc.next(TextFrames.Frame);
        Texts.Attributes(F.text, F.carloc.pos, fnt, col, voff);
        Texts.ChangeLooks(T, beg, end, {0,1,2}, fnt, col, voff)
      END
    END
  END CopyLooks; 

  PROCEDURE ChangeFont*;
    VAR S: Texts.Scanner; T: Texts.Text; beg, end: LONGINT;
  BEGIN
    Oberon.GetSelection(T, beg, end, time);
    IF time >= 0 THEN
      Texts.OpenScanner(S, Oberon.Par.text, Oberon.Par.pos); Texts.Scan(S);
      IF S.class = Texts.Name THEN
        Texts.ChangeLooks(T, beg, end, {0}, Fonts.This(S.s), 0, 0)
      END
    END
  END ChangeFont;

  PROCEDURE ChangeColor*;
    VAR S: Texts.Scanner;
      T: Texts.Text;
      col: INTEGER;
      beg, end, time: LONGINT;
  BEGIN Texts.OpenScanner(S, Oberon.Par.text, Oberon.Par.pos); Texts.Scan(S);
    IF S.class = Texts.Int THEN
      col := S.i; Oberon.GetSelection(T, beg, end, time);
      IF time >= 0 THEN Texts.ChangeLooks(T, beg, end, {1}, NIL, col, 0) END
    END
  END ChangeColor;

  PROCEDURE ChangeOffset*;
    VAR S: Texts.Scanner;
      T: Texts.Text;
      voff: INTEGER; ch: CHAR;
      beg, end, time: LONGINT;
  BEGIN Texts.OpenScanner(S, Oberon.Par.text, Oberon.Par.pos); Texts.Scan(S);
    IF S.class = Texts.Int THEN
      voff := S.i; Oberon.GetSelection(T, beg, end, time);
      IF time >= 0 THEN Texts.ChangeLooks(T, beg, end, {2}, NIL, voff, 0) END
    END
  END ChangeOffset;

  PROCEDURE ShowPos(F: TextFrames.Frame; pos: INTEGER);
    VAR R: Texts.Reader; ch: CHAR; p, n: INTEGER; Fst, Mid, Lst: Line;

    PROCEDURE CountForw(l: Line): INTEGER;
      VAR cnt: INTEGER;
    BEGIN cnt := 0; REPEAT l := l.n; INC(cnt) UNTIL l = NIL
    RETURN cnt
    END CountForw;

    PROCEDURE CountBack(l: Line): INTEGER;
      VAR cnt: INTEGER; c: BOOLEAN;
    BEGIN cnt := -1; REPEAT c := l.c; l := l.p; INC(cnt) UNTIL l = NIL;
      IF ~ c THEN DEC(cnt) END
    RETURN cnt
    END CountBack;

    PROCEDURE Forward(t: Texts.Text; p: INTEGER; line: Line);
      VAR R: Texts.Reader; ch: CHAR; h: INTEGER;
    BEGIN Texts.OpenReader(R, t, p); h := 0;
      REPEAT Texts.Read(R, ch);
        IF R.eot THEN ch := CR
        ELSIF (ch # CR) & (h < R.fnt.height) THEN
          h := R.fnt.height
        END
      UNTIL ch = CR;
      line.b := p; line.e := Texts.Pos(R) - 1; line.h := h
    END Forward;

    PROCEDURE Backward(t: Texts.Text; p: INTEGER; line: Line; VAR fst: Line); (* 0 < p *)
      VAR R: Texts.Reader; ch: CHAR; b, n, h: INTEGER; l: Line;
    BEGIN l := NIL;
      REPEAT IF 64 < p THEN n := p - 64 ELSE n := 0 END;
        Texts.OpenReader(R, t, n); h := 0;
        REPEAT Texts.Read(R, ch);
          IF ch = CR THEN
            IF l = NIL
            THEN NEW(l); l.p := NIL; l.n := NIL; l.b := n; l.c := FALSE; fst := l
            ELSE NEW(l.n); l.n.p := l; l := l.n; l.n := NIL; l.b := b; l.c := TRUE
            END;
            l.e := Texts.Pos(R) - 1; b := l.e + 1;
            IF h = 0 THEN l.h := R.fnt.height ELSE l.h := h END
          ELSIF h < R.fnt.height THEN
            h := R.fnt.height
          END
        UNTIL Texts.Pos(R) - 1 = p;
        p := n
      UNTIL (p = 0) OR (l # NIL);
      IF l = NIL THEN line.b := 0; fst := line
      ELSE l.n := line; line.p := l; line.b := b
      END;
      IF h = 0 THEN line.h := R.fnt.height ELSE line.h := h END;
      line.c := TRUE
    END Backward;

    PROCEDURE Height(l: Line): INTEGER;
      VAR h: INTEGER;
    BEGIN IF l.c THEN h := l.h ELSE h := 0 END; l := l.n;
      WHILE l # NIL DO INC(h, l.h); l := l.n END
    RETURN h
    END Height;
  BEGIN
    IF (pos < F.org) OR (TextFrames.Pos(F, F.X + F.W, F.Y) < pos) THEN
      IF pos <= 0 THEN
        TextFrames.Show(F, 0)
      ELSE
        NEW(Mid); Fst := Mid; Lst := Mid;
        Forward(F.text, pos, Lst); Backward(F.text, pos - 1, Fst, Fst);
        IF Mid.h <= F.H - F.bot - F.top THEN p := CountBack(Mid); n := CountForw(Mid);
          WHILE ((0 < Fst.b) OR (Lst.e < F.text.len)) & (Height(Fst) < F.H - F.bot - F.top) OR ((2 * (n - 1) < p) & (Lst.e < F.text.len)) DO
            IF (2 * (n - 1) <= p) OR (Fst.b = 0) THEN NEW(Lst.n); Lst.n.p := Lst; Lst := Lst.n; Lst.n := NIL; Lst.c := TRUE;
              Forward(F.text, Lst.p.e + 1, Lst); INC(n)
            ELSE Backward(F.text, Fst.b - 1, Fst, Fst); p := CountBack(Mid)
            END
          END;
          p := CountBack(Mid); n := CountForw(Mid);
          WHILE (F.H - F.bot - F.top < Height(Fst)) & (Fst # Lst) DO
            IF (2 * (n - 1) <= p) OR (Lst = Mid) THEN DEC(p);
              IF Fst.c THEN Fst := Fst.n ELSE Fst := Fst.n.n END; Fst.p := NIL
            ELSE DEC(n); Lst := Lst.p; Lst.n := NIL
            END
          END;
          IF Fst.c THEN p := Fst.b ELSE p := Fst.n.b END
        ELSE IF 256 < pos THEN p := pos - 256 ELSE p := 0 END;
          Texts.OpenReader(R, F.text, p);
          REPEAT Texts.Read(R, ch);
            IF ch = 0DX THEN p := Texts.Pos(R) - 1 END
          UNTIL Texts.Pos(R) = pos
        END;
        TextFrames.Show(F, p)
      END
    END
  END ShowPos;

  PROCEDURE Search*;  (*uses global variables M, pat, d for Boyer-Moore search*)
    VAR Text: TextFrames.Frame;
      V: Viewers.Viewer;
      R: Texts.Reader;
      T: Texts.Text;
      pos, beg, end, prevTime, len: LONGINT; n, i, j: INTEGER;
      buf: ARRAY 32 OF CHAR;

    PROCEDURE Forward(n: INTEGER; VAR R: Texts.Reader; VAR buf: ARRAY OF CHAR);
      VAR m: INTEGER; j: INTEGER;
    BEGIN m := M - n; j := 0;
      WHILE j # m DO buf[j] := buf[n + j]; INC(j) END;
      WHILE j # M DO Texts.Read(R, buf[j]); INC(j) END
    END Forward;

  BEGIN V := Oberon.Par.vwr;
    IF Oberon.Par.frame # V.dsc THEN V := Oberon.FocusViewer END;
    IF (V.dsc # NIL) & (V.dsc.next IS TextFrames.Frame) THEN
      Text := V.dsc.next(TextFrames.Frame);
      prevTime := time; Oberon.GetSelection(T, beg, end, time);
      IF time > prevTime THEN
        Texts.OpenReader(R, T, beg);
        i := 0; pos := beg;
        REPEAT Texts.Read(R, pat[i]); INC(i); INC(pos)
        UNTIL (i = maxlen) OR (pos = end);
        M := i; j := 0;
        WHILE j # 256 DO d[j] := M; INC(j) END;
        j := 0;
        WHILE j # M - 1 DO d[ORD(pat[j])] := M - 1 - j; INC(j) END
      END;
      IF Text.hasCar THEN pos := Text.carloc.pos ELSE pos := 0 END;
      len := Text.text.len;
      Texts.OpenReader(R, Text.text, pos);
      Forward(M, R, buf); INC(pos, M);
      j := M;
      REPEAT DEC(j) UNTIL (j < 0) OR (buf[j] # pat[j]);
      WHILE (j >= 0) & (pos < len) DO
        n := d[ORD(buf[M-1])]; Forward(n, R, buf); INC(pos, n); j := M;
        REPEAT DEC(j) UNTIL (j < 0) OR (buf[j] # pat[j])
      END ;
      IF j < 0 THEN
        TextFrames.RemoveSelection(Text); TextFrames.RemoveCaret(Text);
        Oberon.RemoveMarks(Text.X, Text.Y, Text.W, Text.H);
        ShowPos(Text, pos); Oberon.PassFocus(V);
        TextFrames.SetCaret(Text, pos)
      END
    END
  END Search;

  PROCEDURE Locate*;
    VAR Text: TextFrames.Frame;
      T: Texts.Text; S: Texts.Scanner;
      V: Viewers.Viewer;
      name: ARRAY 32 OF CHAR;
      beg, end, time, pos, i, X, Y: LONGINT;
  BEGIN
    V := Oberon.FocusViewer;
    IF (V.dsc # NIL) & (V.dsc.next IS TextFrames.Frame) THEN
      Oberon.GetSelection(T, beg, end, time);
      IF time >= 0 THEN
        Texts.OpenScanner(S, T, beg);
        REPEAT Texts.Scan(S) UNTIL (S.class >= Texts.Int); (*skip names*)
        IF S.class = Texts.Int THEN
          pos := S.i;
          IF (S.nextCh = "@") THEN
            Texts.Scan(S); Texts.Scan(S);
            IF (S.class = Texts.Name) THEN
              i := 0;
              WHILE S.s[i] # 0X DO name[i] := S.s[i]; INC(i) END;
              name[i] := "."; name[i+1] := "M"; name[i+2] := "o"; name[i+3] := "d"; name[i+4] := 0X;
              Texts.OpenScanner(S, V.dsc(TextFrames.Frame).text, 0);
              Texts.Scan(S);
              IF (S.class = Texts.Name) & (S.s # name) THEN
                Oberon.AllocateUserViewer(V.X, X, Y);
                V := MenuViewers.New(
                  TextFrames.NewMenu(name, StandardMenu),
                  TextFrames.NewText(TextFrames.Text(name), 0),
                  TextFrames.menuH, X, Y)
              END
            END
          END;
          Text := V.dsc.next(TextFrames.Frame);
          TextFrames.RemoveSelection(Text);
          TextFrames.RemoveCaret(Text);
          Oberon.RemoveMarks(Text.X, Text.Y, Text.W, Text.H);
          ShowPos(Text, pos);
          Oberon.PassFocus(V);
          TextFrames.SetCaret(Text, pos)
        END
      END
    END
  END Locate;

  PROCEDURE Recall*;
    VAR Menu, Main: Display.Frame;
      buf: Texts.Buffer;
      V: Viewers.Viewer;
      pos: LONGINT;
      M: TextFrames.Frame;
  BEGIN V := Oberon.FocusViewer;
    IF (V # NIL) & (V IS MenuViewers.Viewer) THEN
      Menu := V.dsc; Main := V.dsc.next;
      IF Main IS TextFrames.Frame THEN
        M := Main(TextFrames.Frame);
        IF M.hasCar THEN
          TextFrames.Recall(buf);
          pos := M.carloc.pos + buf.len;
          Texts.Insert(M.text, M.carloc.pos, buf);
          TextFrames.SetCaret(M, pos)
        END
      ELSIF Menu IS TextFrames.Frame THEN
        M := Menu(TextFrames.Frame);
        IF M.hasCar THEN
          TextFrames.Recall(buf);
          pos := M.carloc.pos + buf.len;
          Texts.Insert(M.text, M.carloc.pos, buf);
          TextFrames.SetCaret(M, pos)
        END
      END
    END
  END Recall;

BEGIN Texts.OpenWriter(W)
END Edit.


