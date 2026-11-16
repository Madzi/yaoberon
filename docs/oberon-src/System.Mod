MODULE System; (*JG 3.10.90 / NW 12.10.93 / NW 20.6.2016*)
  IMPORT SYSTEM, Kernel, FileDir, Files, Modules,
    Input, Display, Viewers, Fonts, Texts, Oberon, MenuViewers, TextFrames;

  CONST
    StandardMenu = "System.Close System.Copy System.Grow Edit.Search Edit.Store";
    LogMenu = "Edit.Locate Edit.Search System.Copy System.Grow System.Clear";

  VAR W: Texts.Writer;
    AbortTrampoline: ARRAY 6 OF INTEGER;
    pat: ARRAY 32 OF CHAR;
    inTrap: BOOLEAN;

  PROCEDURE GetArg(VAR S: Texts.Scanner);
    VAR T: Texts.Text; beg, end, time: LONGINT;
  BEGIN Texts.OpenScanner(S, Oberon.Par.text, Oberon.Par.pos); Texts.Scan(S);
    IF (S.class = Texts.Char) & (S.c = "^") THEN
      Oberon.GetSelection(T, beg, end, time);
      IF time >= 0 THEN Texts.OpenScanner(S, T, beg); Texts.Scan(S) END
    END
  END GetArg;

  PROCEDURE EndLine;
  BEGIN Texts.WriteLn(W); Texts.Append(Oberon.Log, W.buf)
  END EndLine;

  (* ------------- Toolbox for system control ---------------*)

  PROCEDURE SetUser*;
    VAR i: INTEGER; ch: CHAR;
      user: ARRAY 8 OF CHAR;
      password: ARRAY 16 OF CHAR;
  BEGIN i := 0; Input.Read(ch);
    WHILE (ch # "/") & (i < 7) DO user[i] := ch; INC(i); Input.Read(ch) END;
    user[i] := 0X; i := 0; Input.Read(ch);
    WHILE (ch > " ") & (i < 15) DO password[i] := ch; INC(i); Input.Read(ch) END;
    password[i] := 0X; Oberon.SetUser(user, password)
  END SetUser;

  PROCEDURE SetFont*;
    VAR S: Texts.Scanner;
  BEGIN GetArg(S);
    IF S.class = Texts.Name THEN Oberon.SetFont(Fonts.This(S.s)) END
  END SetFont;

  PROCEDURE SetColor*;
    VAR S: Texts.Scanner;
  BEGIN GetArg(S);
    IF S.class = Texts.Int THEN Oberon.SetColor(S.i) END
  END SetColor;

  PROCEDURE SetOffset*;
    VAR S: Texts.Scanner;
  BEGIN GetArg(S);
    IF S.class = Texts.Int THEN Oberon.SetOffset(S.i) END
  END SetOffset;
  
  PROCEDURE Date*;
    VAR S: Texts.Scanner;
      dt, hr, min, sec, yr, mo, day: LONGINT;
  BEGIN Texts.OpenScanner(S, Oberon.Par.text, Oberon.Par.pos); Texts.Scan(S);
    IF S.class = Texts.Int THEN (*set clock*)
      day := S.i; Texts.Scan(S); mo := S.i; Texts.Scan(S); yr := S.i; Texts.Scan(S);
      hr := S.i; Texts.Scan(S); min := S.i; Texts.Scan(S); sec := S.i;
      dt := ((((yr*16 + mo)*32 + day)*32 + hr)*64 + min)*64 + sec;
      Kernel.SetClock(dt)
    ELSE (*read clock*) Texts.WriteString(W, "System.Clock");
      dt := Oberon.Clock(); Texts.WriteClock(W, dt); EndLine
    END
  END Date;

  PROCEDURE Collect*;
  BEGIN Oberon.Collect(0)
  END Collect;

  PROCEDURE Quit*;
  BEGIN Kernel.Quit
  END Quit;

  PROCEDURE Reboot*;
  BEGIN Kernel.HardReset
  END Reboot;

  (* ------------- Toolbox for standard display ---------------*)

  PROCEDURE Open*;  (*open viewer in system track*)
    VAR X, Y: INTEGER;
      V: Viewers.Viewer;
      S: Texts.Scanner;
  BEGIN GetArg(S);
    IF S.class = Texts.Name THEN
      Oberon.AllocateSystemViewer(Oberon.Par.vwr.X, X, Y);
      V := MenuViewers.New(
        TextFrames.NewMenu(S.s, StandardMenu),
        TextFrames.NewText(TextFrames.Text(S.s), 0), TextFrames.menuH, X, Y)
    END
  END Open;

  PROCEDURE Clear*;  (*clear Log*)
    VAR T: Texts.Text; F: Display.Frame; buf: Texts.Buffer;
  BEGIN F := Oberon.Par.frame;
    IF (F # NIL) & (F.next IS TextFrames.Frame) & (F = Oberon.Par.vwr.dsc) THEN
      NEW(buf); Texts.OpenBuf(buf); T := F.next(TextFrames.Frame).text; Texts.Delete(T, 0, T.len, buf)
    END
  END Clear;

  PROCEDURE Close*;
    VAR V: Viewers.Viewer;
  BEGIN
    IF Oberon.Par.frame = Oberon.Par.vwr.dsc THEN V := Oberon.Par.vwr
    ELSE V := Oberon.MarkedViewer()
    END;
    Viewers.Close(V)
  END Close;

  PROCEDURE CloseTrack*;
    VAR V: Viewers.Viewer;
  BEGIN V := Oberon.MarkedViewer(); Viewers.CloseTrack(V.X)
  END CloseTrack;

  PROCEDURE Recall*;
    VAR V: Viewers.Viewer; M: Viewers.ViewerMsg;
  BEGIN Viewers.Recall(V);
    IF (V#NIL) & (V.state = 0) THEN
      Viewers.Open(V, V.X, V.Y + V.H); M.id := Viewers.restore; V.handle(V, M)
    END
  END Recall;

  PROCEDURE Copy*;
    VAR V, V1: Viewers.Viewer; M: Oberon.CopyMsg; N: Viewers.ViewerMsg;
  BEGIN V := Oberon.Par.vwr; V.handle(V, M); V1 := M.F(Viewers.Viewer);
    Viewers.Open(V1, V.X, V.Y + V.H DIV 2);
    N.id := Viewers.restore; V1.handle(V1, N)
  END Copy;

  PROCEDURE Grow*;
    VAR V, V1: Viewers.Viewer; M: Oberon.CopyMsg; N: Viewers.ViewerMsg;
      DW, DH: INTEGER;
  BEGIN V := Oberon.Par.vwr;
    DW := Oberon.DisplayWidth(V.X); DH := Oberon.DisplayHeight(V.X);
    IF V.H < DH - Viewers.minH THEN Oberon.OpenTrack(V.X, V.W)
    ELSIF V.W < DW THEN Oberon.OpenTrack(Oberon.UserTrack(V.X), DW)
    END;
    IF (V.H < DH - Viewers.minH) OR (V.W < DW) THEN
      V.handle(V, M); V1 := M.F(Viewers.Viewer);
      Viewers.Open(V1, V.X, DH);;
      N.id := Viewers.restore; V1.handle(V1, N)
    END
  END Grow;

  (* ------------- Toolbox for module management ---------------*)

  PROCEDURE Free1(VAR S: Texts.Scanner);
  BEGIN Texts.WriteString(W, S.s); Texts.WriteString(W, " unloading");
    Modules.Free(S.s);
    IF Modules.res # 0 THEN Texts.WriteString(W, " failed"); Oberon.SetRetVal(1) END;
    EndLine
  END Free1;

  PROCEDURE Free*;
    VAR T: Texts.Text;
      beg, end, time: LONGINT;
      S: Texts.Scanner;
  BEGIN Texts.WriteString(W, "System.Free"); EndLine;
    Texts.OpenScanner(S, Oberon.Par.text, Oberon.Par.pos); Texts.Scan(S);
    IF (S.class = Texts.Char) & (S.c = "^") THEN
      Oberon.GetSelection(T, beg, end, time);
      IF time >= 0 THEN Texts.OpenScanner(S, T, beg); Texts.Scan(S);
        IF S.class = Texts.Name THEN Free1(S) END
      END
    ELSE
      WHILE S.class = Texts.Name DO Free1(S); Texts.Scan(S) END
    END ;
    Oberon.Collect(0)
  END Free;

  PROCEDURE FreeFonts*;
  BEGIN Texts.WriteString(W, "System.FreeFonts"); Fonts.Free; EndLine
  END FreeFonts;

  (* ------------- Toolbox of file system ---------------*)

  PROCEDURE List(name: FileDir.FileName; adr: LONGINT; VAR cont: BOOLEAN);
    VAR i0, i, j0, j, date, length: INTEGER;
  BEGIN
    i := 0;
    WHILE (pat[i] > "*") & (pat[i] = name[i]) DO INC(i) END ;
    IF (pat[i] = 0X) & (name[i] = 0X) THEN i0 := i; j0 := i
    ELSIF pat[i] = "*" THEN
      i0 := i; j0 := i+1;
      WHILE name[i0] # 0X DO
        i := i0; j := j0;
        WHILE (name[i] # 0X) & (name[i] = pat[j]) DO INC(i); INC(j) END ;
        IF pat[j] = 0X THEN
          IF name[i] = 0X THEN (*match*) j0 := j ELSE INC(i0) END
        ELSIF pat[j] = "*" THEN i0 := i; j0 := j+1
        ELSE INC(i0)
        END
      END
    END ;
    IF (name[i0] = 0X) & (pat[j0] = 0X) THEN (*found*)
      Texts.WriteString(W, name);
      IF pat[j0+1] = "!" THEN (*option*)
        FileDir.GetAttributes(adr, date, length);
        Texts.Write(W, 9X); Texts.WriteClock(W, date);
        Texts.WriteInt(W, length, 8);
        (*Texts.WriteHex(W, adr)*)
      END ;
      Texts.WriteLn(W)
    END
  END List;

  PROCEDURE Directory*;
    VAR X, Y, i: INTEGER; ch: CHAR;
      R: Texts.Reader;
      T, t: Texts.Text;
      V: Viewers.Viewer;
      beg, end, time: LONGINT;
      pre: ARRAY 32 OF CHAR;
  BEGIN Texts.OpenReader(R, Oberon.Par.text, Oberon.Par.pos); Texts.Read(R, ch);
    WHILE ch = " " DO Texts.Read(R, ch) END;
    IF (ch = "^") OR (ch = 0DX) THEN
      Oberon.GetSelection(T, beg, end, time);
      IF time >= 0 THEN
        Texts.OpenReader(R, T, beg); Texts.Read(R, ch);
        WHILE ch <= " " DO Texts.Read(R, ch) END
      END
    END ;
    i := 0;
    WHILE ch > "!" DO pat[i] := ch; INC(i); Texts.Read(R, ch) END;
    pat[i] := 0X;
    IF ch = "!" THEN pat[i+1] := "!" END ;  (*directory option*)
    i := 0;
    WHILE pat[i] > "*" DO pre[i] := pat[i]; INC(i) END;
    pre[i] := 0X;
    Oberon.AllocateSystemViewer(Oberon.Par.vwr.X, X, Y); t := TextFrames.Text("");
    V := MenuViewers.New(
      TextFrames.NewMenu("System.Directory", StandardMenu),
      TextFrames.NewText(t, 0), TextFrames.menuH, X, Y);
    FileDir.Enumerate(pre, List); Texts.Append(t, W.buf)
  END Directory;

  PROCEDURE CopyFiles*;
    VAR f, g: Files.File; Rf, Rg: Files.Rider; ch: CHAR;
      name: ARRAY 32 OF CHAR;
      S: Texts.Scanner;
  BEGIN GetArg(S);
    Texts.WriteString(W, "System.CopyFiles"); EndLine;
    WHILE S.class = Texts.Name DO
      name := S.s; Texts.Scan(S);
      IF (S.class = Texts.Char) & (S.c = "=") THEN Texts.Scan(S);
        IF (S.class = Texts.Char) & (S.c = ">") THEN Texts.Scan(S);
          IF S.class = Texts.Name THEN
            Texts.WriteString(W, name); Texts.WriteString(W, " => "); Texts.WriteString(W, S.s);
            Texts.WriteString(W, " copying"); Texts.Append(Oberon.Log, W.buf);
            f := Files.Old(name);
            IF f # NIL THEN g := Files.New(S.s);
              Files.Set(Rf, f, 0); Files.Set(Rg, g, 0); Files.Read(Rf, ch);
              WHILE ~Rf.eof DO Files.Write(Rg, ch); Files.Read(Rf, ch) END;
              Files.Register(g)
            ELSE Texts.WriteString(W, " failed"); Oberon.SetRetVal(1)
            END ;
            EndLine
          END
        END
      END ;
      Texts.Scan(S)
    END
  END CopyFiles;

  PROCEDURE RenameFiles*;
    VAR res: INTEGER;
      name: ARRAY 32 OF CHAR;
      S: Texts.Scanner;
  BEGIN GetArg(S);
    Texts.WriteString(W, "System.RenameFiles"); EndLine;
    WHILE S.class = Texts.Name DO
      name := S.s; Texts.Scan(S);
      IF (S.class = Texts.Char) & (S.c = "=") THEN Texts.Scan(S);
        IF (S.class = Texts.Char) & (S.c = ">") THEN Texts.Scan(S);
          IF S.class = Texts.Name THEN
            Texts.WriteString(W, name); Texts.WriteString(W, " => "); Texts.WriteString(W, S.s);
            Texts.WriteString(W, " renaming"); Files.Rename(name, S.s, res);
            IF res > 1 THEN Texts.WriteString(W, " failed"); Oberon.SetRetVal(1) END;
            EndLine
          END
        END
      END ;
      Texts.Scan(S)
    END
  END RenameFiles;

  PROCEDURE DeleteFiles*;
    VAR res: INTEGER; S: Texts.Scanner;
  BEGIN GetArg(S);
    Texts.WriteString(W, "System.DeleteFiles"); EndLine;
    WHILE S.class = Texts.Name DO
      Texts.WriteString(W, S.s); Texts.WriteString(W, " deleting");
      Files.Delete(S.s, res);
      IF res # 0 THEN Texts.WriteString(W, " failed"); Oberon.SetRetVal(1) END;
      EndLine; Texts.Scan(S)
    END
  END DeleteFiles;

  (* ------------- Toolbox for system inspection ---------------*)

  PROCEDURE Watch*;
  BEGIN Texts.WriteString(W, "System.Watch"); Texts.WriteLn(W);
    Texts.WriteString(W, "  Modules space (bytes)"); Texts.WriteInt(W, Modules.AllocPtr, 8);
    Texts.WriteInt(W, Modules.AllocPtr * 100 DIV Kernel.heapOrg, 4); Texts.Write(W, "%"); EndLine;
    Texts.WriteString(W, "  Heap speace"); Texts.WriteInt(W, Kernel.allocated, 8);
    Texts.WriteInt(W, Kernel.allocated * 100 DIV (Kernel.heapLim - Kernel.heapOrg), 4); Texts.Write(W, "%"); EndLine;
    Texts.WriteString(W, "  Disk sectors "); Texts.WriteInt(W, Kernel.NofSectors, 4);
    Texts.WriteInt(W, Kernel.NofSectors * 100 DIV 10000H, 4); Texts.Write(W, "%"); EndLine;
    Texts.WriteString(W, "  Tasks"); Texts.WriteInt(W, Oberon.NofTasks, 4); EndLine
  END Watch;

  PROCEDURE SetColorTheme*;
    VAR S: Texts.Scanner;
      doit: BOOLEAN;
      mix: BOOLEAN; fc, bc, mbc, tbc, smc, sbc, cc, cmc, sc, uc: INTEGER;
  BEGIN GetArg(S);
    IF (S.class = Texts.Char) & (S.c = "*") THEN mix := TRUE; Texts.Scan(S) ELSE mix := FALSE END;
    IF S.class = Texts.Int THEN fc := S.i; Texts.Scan(S) ELSE fc := Display.white END;
    IF S.class = Texts.Int THEN bc := S.i; Texts.Scan(S) ELSE bc := Display.black END;
    IF S.class = Texts.Int THEN mbc := S.i; Texts.Scan(S) ELSE mbc := fc END;
    IF S.class = Texts.Int THEN tbc := S.i; Texts.Scan(S) ELSE tbc := bc END;
    IF S.class = Texts.Int THEN smc := S.i; Texts.Scan(S) ELSE smc := fc END;
    IF S.class = Texts.Int THEN sbc := S.i; Texts.Scan(S) ELSE sbc := fc END;
    IF S.class = Texts.Int THEN cc := S.i; Texts.Scan(S); ELSE cc := Display.white END;
    IF S.class = Texts.Int THEN cmc := S.i; Texts.Scan(S) ELSE cmc := cc END;
    IF S.class = Texts.Int THEN sc := S.i; Texts.Scan(S) ELSE sc := cc END;
    IF S.class = Texts.Int THEN uc := S.i; Texts.Scan(S) ELSE uc := cc END;
    IF (S.class = Texts.Char) & (S.c = "@") THEN
      Texts.Scan(S); doit := (S.class = Texts.Int) & (S.i = Display.Depth)
    ELSE
      doit := TRUE
    END;
    IF doit THEN
      Oberon.SetColorTheme(mix, fc, bc, mbc, tbc, smc, sbc, cc, cmc, sc, uc)
    END
  END SetColorTheme;

  PROCEDURE ShowModules*;
    VAR T: Texts.Text;
      V: Viewers.Viewer;
      M: Modules.Module;
      X, Y: INTEGER;
  BEGIN T := TextFrames.Text("");
    Oberon.AllocateSystemViewer(Oberon.Par.vwr.X, X, Y);
    V := MenuViewers.New(TextFrames.NewMenu("System.ShowModules", StandardMenu),
        TextFrames.NewText(T, 0), TextFrames.menuH, X, Y);
    M := Modules.root;
    WHILE M # NIL DO
      IF M.name[0] # 0X THEN
        Texts.WriteString(W, M.name); Texts.Write(W, 9X); Texts.WriteHex(W, ORD(M));
        Texts.WriteHex(W, M.code); Texts.WriteInt(W, M.refcnt, 4)
      ELSE Texts.WriteString(W, "---")
      END ;
      Texts.WriteLn(W); M := M.next
    END;
    Texts.Append(T, W.buf)
  END ShowModules;

  PROCEDURE MakeAbortTrampoline;
  BEGIN ASSERT(SYSTEM.ADR(Kernel.stackOrg) < 80000H);
    AbortTrampoline[0] := 040000000H;                                (* MOV  R0, 0                       *)
    AbortTrampoline[1] := 080000000H + SYSTEM.ADR(Kernel.stackOrg);  (* LDR  R0, [R0 + Kernel.stackOrg]  *)
    AbortTrampoline[2] := 001E90000H;                                (* SUB  R1, SP, R0                  *)
    AbortTrampoline[3] := 0E6000001H;                                (* BLE  +1                          *)
    AbortTrampoline[4] := 0E0000000H;                                (* MOV  SP, R0                      *)
    Kernel.InvalidateCodeCache(SYSTEM.ADR(AbortTrampoline[0]));
  END MakeAbortTrampoline;

  PROCEDURE ShowCommands*;
    VAR M: Modules.Module;
      comadr: LONGINT; ch: CHAR;
      T: Texts.Text;
      S: Texts.Scanner;
      V: Viewers.Viewer;
      X, Y: INTEGER;
  BEGIN GetArg(S);
    IF S.class = Texts.Name THEN
      Modules.Load(S.s, M);
      IF M # NIL THEN
        Oberon.AllocateSystemViewer(Oberon.Par.vwr.X, X, Y); T := TextFrames.Text("");
        V := MenuViewers.New(TextFrames.NewMenu("System.Commands", StandardMenu),
            TextFrames.NewText(T, 0), TextFrames.menuH, X, Y);
        comadr := M.cmd; SYSTEM.GET(comadr, ch); INC(comadr);
        WHILE ch # 0X DO
          Texts.WriteString(W, S.s); Texts.Write(W, ".");
          REPEAT Texts.Write(W, ch); SYSTEM.GET(comadr, ch); INC(comadr)
          UNTIL ch = 0X;
          WHILE comadr MOD 4 # 0 DO INC(comadr) END ;
          Texts.WriteLn(W); INC(comadr, 4); SYSTEM.GET(comadr, ch); INC(comadr)
        END ;
        Texts.Append(T, W.buf)
      END
    END
  END ShowCommands;

  PROCEDURE SetScreenSize*;
    VAR S: Texts.Scanner;
      w, h: INTEGER;
      tmp: BOOLEAN;
  BEGIN GetArg(S);
    IF S.class = Texts.Int THEN w := S.i; Texts.Scan(S) END;
    IF S.class = Texts.Int THEN
      h := S.i; Texts.Scan(S);
      IF (S.class = Texts.Int) & (S.i IN {1,4,8,16,24}) & Display.SetDepth(S.i) THEN
        IF (w = 0) & (h = 0) THEN w := Oberon.DisplayWidth(0); h := Oberon.DisplayHeight(0) END;
        tmp := Display.SetSize(w, h);
        Oberon.UpdateScreenSize()
      ELSIF Display.SetSize(w, h) THEN
        Oberon.UpdateScreenSize()
      END
    END
  END SetScreenSize;

  PROCEDURE ShowFonts*;
    VAR fnt: Fonts.Font;
  BEGIN Texts.WriteString(W, "System.ShowFonts"); Texts.WriteLn(W); fnt := Fonts.root;
    WHILE fnt # NIL DO
      Texts.Write(W, 9X); Texts.WriteString(W, fnt.name); Texts.WriteLn(W); fnt := fnt.next
    END ;
    Texts.Append(Oberon.Log, W.buf)
  END ShowFonts;

  PROCEDURE CheckStartup(T: Texts.Text; F: TextFrames.Frame; prefix: ARRAY OF CHAR);
    VAR R: Texts.Reader; ch: CHAR; i: INTEGER;
  BEGIN i := 0;
    Texts.OpenReader(R, T, 0);
    Texts.Read(R, ch);
    WHILE (ch # 0X) & (ch <= " ") DO Texts.Read(R, ch) END;
    WHILE (ch # 0X) & (ch = prefix[i]) DO Texts.Read(R, ch); INC(i) END;
    IF prefix[i] = 0X THEN
      WHILE (ch # 0X) & (ch <= " ") DO Texts.Read(R, ch) END;
      TextFrames.Call(F, Texts.Pos(R) - 1, FALSE)
    END
  END CheckStartup;

  PROCEDURE OpenViewers;
    VAR logV, toolV: Viewers.Viewer;
      menu, main: TextFrames.Frame;
      d: LONGINT; X, Y: INTEGER;
  BEGIN d := Kernel.Clock(); Texts.WriteString(W, "Oberon V5  NW 14.4.2013"); EndLine;
    Oberon.AllocateSystemViewer(0, X, Y);
    menu := TextFrames.NewMenu("System.Log", LogMenu);
    main := TextFrames.NewText(Oberon.Log, 0);
    logV := MenuViewers.New(menu, main, TextFrames.menuH, X, Y);
    Oberon.AllocateSystemViewer(0, X, Y);
    menu := TextFrames.NewMenu("System.Tool", StandardMenu);
    main := TextFrames.NewText(TextFrames.Text("System.Tool"), 0);
    toolV := MenuViewers.New(menu, main, TextFrames.menuH, X, Y)
    ;CheckStartup(main.text, main, "@Startup:");
  END OpenViewers;

  PROCEDURE DisarmTrap(adr: INTEGER);
  VAR ins, cnt: INTEGER;
  BEGIN
    SYSTEM.GET(adr, ins); cnt := 0;
    WHILE (cnt < 32) & ((ins DIV 1000000H MOD 100H # 0DEH) OR (ins MOD 100H # 9CH)) DO
      INC(adr, 4); INC(cnt); SYSTEM.GET(adr, ins)
    END;
    IF (cnt < 32) THEN SYSTEM.PUT(adr, ins + 01000000H) END
  END DisarmTrap;

  PROCEDURE ExtendDisplay*;
    VAR V: Viewers.Viewer;
      X, Y, DX, DW, DH: INTEGER;
      S: Texts.Scanner;
  BEGIN GetArg(S);
    IF S.class = Texts.Name THEN
      DX := Viewers.curW; DW := Oberon.DisplayWidth(DX); DH := Oberon.DisplayHeight(DX);
      Oberon.OpenDisplay(DW DIV 8 * 5, DW DIV 8 * 3, DH);
      Oberon.AllocateSystemViewer(DX, X, Y);
      V := MenuViewers.New(
        TextFrames.NewMenu(S.s, StandardMenu),
        TextFrames.NewText(TextFrames.Text(S.s), 0),
        TextFrames.menuH, X, Y)
    END
  END ExtendDisplay;

  PROCEDURE DoCommands*;
    VAR pos: INTEGER; S: Texts.Scanner; run: BOOLEAN;
  BEGIN pos := Oberon.Par.pos; run := TRUE;
    WHILE run DO
      run := FALSE;
      Texts.OpenScanner(S, Oberon.Par.text, pos); Texts.Scan(S);
      IF S.class = Texts.Name THEN
        TextFrames.Call(Oberon.Par.frame(TextFrames.Frame), pos, FALSE);
        IF Oberon.RetVal = 0 THEN
          WHILE (~S.eot) & ~((S.class = Texts.Char) & (S.c = "~")) DO Texts.Scan(S) END;
          pos := Texts.Pos(S); run := ~S.eot
        END
      END
    END
  END DoCommands;

  PROCEDURE Backtrace();
  VAR sp, adr, v, cnt: INTEGER;
    mod: Modules.Module;
  BEGIN
    sp := SYSTEM.REG(14);
    INC(sp, 4); cnt := 0;
    WHILE (sp < Kernel.stackOrg) & (cnt < 64) DO
      SYSTEM.GET(sp, adr);
      DEC(adr, 4);
      IF (adr MOD 4 = 0) & (adr >= 100H) & (adr < Modules.AllocPtr) THEN
        SYSTEM.GET(adr, v);
        IF (v DIV 10000000H MOD 10H = 0DH) OR (v DIV 10000000H MOD 10H = 0FH) THEN (* BL or BLR instruction *)
          IF (v DIV 10000000H MOD 10H = 0FH) THEN (* BL instruction *)
            SYSTEM.GET(adr-4,v); (* Check for dummy Trap/BLR *)
            IF v DIV 1000000H MOD 100H # 0DFH THEN v := 0 END
          END;
          mod := Modules.root;
          WHILE (mod # NIL) & ((adr < mod.code) OR (adr >= mod.imp)) DO mod := mod.next END ;
          IF mod # NIL THEN
            Texts.WriteString(W, "    at ");
            Texts.WriteInt(W, v DIV 100H MOD 10000H, 5);
            Texts.Write(W,"@");
            Texts.WriteString(W, mod.name);
            Texts.Write(W, "+");
            Texts.WriteHex(W, adr - mod.code);
            Texts.WriteString(W, " (");
            Texts.WriteHex(W, adr);
            Texts.Write(W, ")");
            Texts.WriteLn(W); INC(cnt)
          END
        END
      END;
      INC(sp, 4);
    END;
  END Backtrace;

  PROCEDURE Trap(VAR a: INTEGER; b: INTEGER);
    VAR u, v, w: INTEGER; mod: Modules.Module;
  BEGIN u := SYSTEM.REG(15); SYSTEM.GET(u - 4, v);
    SYSTEM.GET(16, w); SYSTEM.PUT(16, w - 1024);
    w := v DIV 10H MOD 10H; (*trap number*)
    IF w = 0 THEN Kernel.New(a, b)
    ELSE (*trap*)
      Oberon.Unsacrifice();
      IF ~inTrap THEN inTrap := TRUE;
      Texts.WriteLn(W); Texts.WriteString(W, "  pos "); Texts.WriteInt(W, v DIV 100H MOD 10000H, 4);
      Texts.WriteString(W, "  TRAP"); Texts.WriteInt(W, w, 4); mod := Modules.root;
      WHILE (mod # NIL) & ((u < mod.code) OR (u >= mod.imp)) DO mod := mod.next END ;
      IF mod # NIL THEN Texts.WriteString(W, " in "); Texts.WriteString(W, mod.name) END ;
      Texts.WriteString(W, " at"); Texts.WriteHex(W, u);
      Texts.WriteLn(W); Backtrace(); Texts.Append(Oberon.Log, W.buf)
      ELSE Oberon.LogDoubleTrap() END; Oberon.Collect(0); inTrap := FALSE; Oberon.Reset
    END
    ;SYSTEM.GET(16, w); SYSTEM.PUT(16, w + 1024);
  END Trap;

  PROCEDURE Abort;
    VAR n: INTEGER;
  BEGIN n := SYSTEM.REG(15); Oberon.Unsacrifice();
    IF ~inTrap THEN inTrap := TRUE;
    Texts.WriteString(W, "  ABORT  "); Texts.WriteHex(W, n);
    Texts.WriteLn(W); Backtrace(); Texts.Append(Oberon.Log, W.buf)
    ELSE Oberon.LogDoubleTrap() END; Oberon.Collect(0); inTrap := FALSE; Oberon.Reset
  END Abort;
  
BEGIN Texts.OpenWriter(W);
  inTrap := FALSE;
  Oberon.OpenLog(TextFrames.Text("")); OpenViewers;
  Kernel.Install(SYSTEM.ADR(Trap), 20H); DisarmTrap(SYSTEM.ADR(Trap)); MakeAbortTrampoline;
  Kernel.Install(SYSTEM.ADR(Abort), SYSTEM.ADR(AbortTrampoline[5]));
  Kernel.Install(SYSTEM.ADR(AbortTrampoline[0]), 0);
END System.
