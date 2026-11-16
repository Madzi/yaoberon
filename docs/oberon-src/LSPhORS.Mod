MODULE LSPhORS; (* NW 19.9.93 / 15.3.2017  Scanner in Oberon-07*)
  IMPORT SYSTEM, Texts, Oberon, RS232, LSPhConstants;

(* Oberon Scanner does lexical analysis. Input is Oberon-Text, output is
  sequence of symbols, i.e identifiers, numbers, strings, and special symbols.
  Recognises all Oberon keywords and skips comments. The keywords are
  recorded in a table.
  Get(sym) delivers next symbol from input text with Reader R.
  Mark(msg) records error and delivers error message with Writer W.
  If Get delivers ident, then the identifier (a string) is in variable id, if int or char
  in ival, if real in rval, and if string in str (and slen) *)
  
  CONST IdLen* = 32;
    NKW = 34;  (*nof keywords*)
    maxExp = 38; stringBufSize = 256;
  
    (*lexical symbols*)
    null = 0; times* = 1; rdiv* = 2; div* = 3; mod* = 4;
    and* = 5; plus* = 6; minus* = 7; or* = 8; eql* = 9;
    neq* = 10; lss* = 11; leq* = 12; gtr* = 13; geq* = 14;
    in* = 15; is* = 16; arrow* = 17; period* = 18;
    char* = 20; int* = 21; real* = 22; false* = 23; true* = 24;
    nil* = 25; string* = 26; not* = 27; lparen* = 28; lbrak* = 29;
    lbrace* = 30; ident* = 31;
    if* = 32; while* = 34; repeat* = 35; case* = 36; for* = 37;
    comma* = 40; colon* = 41; becomes* = 42; upto* = 43; rparen* = 44;
    rbrak* = 45; rbrace* = 46; then* = 47; of* = 48; do* = 49;
    to* = 50; by* = 51; semicolon* = 52; end* = 53; bar* = 54;
    else* = 55; elsif* = 56; until* = 57; return* = 58;
    array* = 60; record* = 61; pointer* = 62; const* = 63; type* = 64;
    var* = 65; procedure* = 66; begin* = 67; import* = 68; module* = 69; eot* = 70;

  TYPE Ident* = ARRAY IdLen OF CHAR;
    Callback* = PROCEDURE(tag, beg, end, value: INTEGER);

  VAR ival*, slen*: LONGINT;  (*results of Get*)
    rval*: REAL;
    id*: Ident;  (*for identifiers*)
    str*: ARRAY stringBufSize OF CHAR;
    errcnt*: INTEGER;

    ch: CHAR;  (*last character read*)
    errpos: LONGINT;
    R: Texts.Reader;
    lspMode*: INTEGER;
    callback: Callback;
    syntaxType, formatType: ARRAY eot+1 OF INTEGER;
    lastIdentStart: INTEGER;
    k: INTEGER;
    KWX: ARRAY 10 OF INTEGER;
    keyTab: ARRAY NKW OF
        RECORD sym: INTEGER; id: ARRAY 12 OF CHAR END;
  
  PROCEDURE CopyId*(VAR ident: Ident);
  BEGIN ident := id
  END CopyId;

  PROCEDURE Pos*(): LONGINT;
  BEGIN RETURN Texts.Pos(R) - 1
  END Pos;

  PROCEDURE Mark*(msg: ARRAY OF CHAR);
    VAR p: LONGINT;
  BEGIN p := Pos();
    IF (p > errpos) & (errcnt < 500) & (lspMode = LSPhConstants.GetModuleInfo) THEN
      RS232.Send(LSPhConstants.Error);
      RS232.SendIntRaw(p);
      RS232.SendStrRaw(msg)
    END ;
    INC(errcnt); errpos := p + 4
  END Mark;

  PROCEDURE Identifier(VAR sym: INTEGER);
    VAR i, k: INTEGER;
  BEGIN i := 0;
    REPEAT
      IF i < IdLen-1 THEN id[i] := ch; INC(i) END ;
      Texts.Read(R, ch)
    UNTIL (ch < "0") & ((lspMode # LSPhConstants.AutoComplete) OR (ch # 3X)) OR (ch > "9") & (ch < "A") OR (ch > "Z") & (ch < "a") OR (ch > "z");
    id[i] := 0X; 
    IF i < 10 THEN k := KWX[i-1];  (*search for keyword*)
      WHILE (id # keyTab[k].id) & (k < KWX[i]) DO INC(k) END ;
      IF k < KWX[i] THEN sym := keyTab[k].sym ELSE sym := ident END
    ELSE sym := ident
    END
  END Identifier;

  PROCEDURE String;
    VAR i: INTEGER;
  BEGIN i := 0; Texts.Read(R, ch);
    WHILE ~R.eot & (ch # 22X) DO
      IF ch >= " " THEN
        IF i < stringBufSize-1 THEN str[i] := ch; INC(i) ELSE Mark("string too long") END ;
      END ;
      Texts.Read(R, ch)
    END ;
    str[i] := 0X; INC(i); Texts.Read(R, ch); slen := i
  END String;

  PROCEDURE HexString;
    VAR i, m, n: INTEGER;
  BEGIN i := 0; Texts.Read(R, ch);
    WHILE ~R.eot & (ch # "$") DO
      WHILE ~R.eot  & (ch <= " ") DO Texts.Read(R, ch) END ;  (*skip*)
      IF ("0" <= ch) & (ch <= "9") THEN m := ORD(ch) - 30H
      ELSIF ("A" <= ch) & (ch <= "F") THEN m := ORD(ch) - 37H
      ELSE m := 0; Mark("hexdig expected")
      END ;
      Texts.Read(R, ch);
      IF ("0" <= ch) & (ch <= "9") THEN n := ORD(ch) - 30H
      ELSIF ("A" <= ch) & (ch <= "F") THEN n := ORD(ch) - 37H
      ELSE n := 0; Mark("hexdig expected")
      END ;
      IF i < stringBufSize THEN str[i] := CHR(m*10H + n); INC(i) ELSE Mark("string too long") END ;
      Texts.Read(R, ch)
    END ;
    Texts.Read(R, ch); slen := i  (*no 0X appended!*)
  END HexString;

  PROCEDURE Ten(e: LONGINT): REAL;
    VAR x, t: REAL;
  BEGIN x := 1.0; t := 10.0;
    WHILE e > 0 DO
      IF ODD(e) THEN x := t * x END ;
      t := t * t; e := e DIV 2
    END ;
    RETURN x
  END Ten;

  PROCEDURE Number(VAR sym: INTEGER);
    CONST max = 2147483647 (*2^31 - 1*);
    VAR i, k, e, n, s, h: LONGINT; x: REAL;
      d: ARRAY 16 OF INTEGER;
      negE: BOOLEAN;
  BEGIN ival := 0; i := 0; n := 0; k := 0;
    REPEAT
      IF n < 16 THEN d[n] := ORD(ch)-30H; INC(n) ELSE Mark("too many digits"); n := 0 END ;
      Texts.Read(R, ch)
    UNTIL (ch < "0") OR (ch > "9") & (ch < "A") OR (ch > "F");
    IF (ch = "H") OR (ch = "R") OR (ch = "X") THEN  (*hex*)
      REPEAT h := d[i];
        IF h >= 10 THEN h := h-7 END ;
        k := k*10H + h; INC(i) (*no overflow check*)
      UNTIL i = n;
      IF ch = "X" THEN sym := char;
        IF k < 100H THEN ival := k ELSE Mark("illegal value"); ival := 0 END
      ELSIF ch = "R" THEN sym := real; rval := SYSTEM.VAL(REAL, k)
      ELSE sym := int; ival := k
      END ;
      Texts.Read(R, ch)
    ELSIF ch = "." THEN
      Texts.Read(R, ch);
      IF ch = "." THEN (*double dot*) ch := 7FX;  (*decimal integer*)
        REPEAT
          IF d[i] < 10 THEN
            IF k <= (max-d[i]) DIV 10 THEN k := k *10 + d[i] ELSE Mark("too large"); k := 0 END
          ELSE Mark("bad integer")
          END ;
          INC(i)
        UNTIL i = n;
        sym := int; ival := k
      ELSE (*real number*) x := 0.0; e := 0;
        REPEAT  (*integer part*) x := x * 10.0 + FLT(d[i]); INC(i) UNTIL i = n;
        WHILE (ch >= "0") & (ch <= "9") DO  (*fraction*)
          x := x * 10.0 + FLT(ORD(ch) - 30H); DEC(e); Texts.Read(R, ch)
        END ;
        IF (ch = "E") OR (ch = "D") THEN  (*scale factor*)
          Texts.Read(R, ch); s := 0; 
          IF ch = "-" THEN negE := TRUE; Texts.Read(R, ch)
          ELSE negE := FALSE;
            IF ch = "+" THEN Texts.Read(R, ch) END
          END ;
          IF (ch >= "0") & (ch <= "9") THEN
            REPEAT s := s*10 + ORD(ch)-30H; Texts.Read(R, ch)
            UNTIL (ch < "0") OR (ch >"9");
            IF negE THEN e := e-s ELSE e := e+s END
          ELSE Mark("digit?")
          END
        END ;
        IF e < 0 THEN
          IF e >= -maxExp THEN x := x / Ten(-e) ELSE x := 0.0 END
        ELSIF e > 0 THEN
          IF e <= maxExp THEN x := Ten(e) * x ELSE x := 0.0; Mark("too large") END
        END ;
        sym := real; rval := x
      END
    ELSE  (*decimal integer*)
      REPEAT
        IF d[i] < 10 THEN
          IF k <= (max-d[i]) DIV 10 THEN k := k*10 + d[i] ELSE Mark("too large"); k := 0 END
        ELSE Mark("bad integer")
        END ;
        INC(i)
      UNTIL i = n;
      sym := int; ival := k
    END
  END Number;

  PROCEDURE SetMode* (m: INTEGER; cb: Callback);
  BEGIN lspMode := m; callback := cb;
  END SetMode;

  PROCEDURE ModuleTag*(tag: INTEGER);
  BEGIN
    IF lspMode = LSPhConstants.GetModuleInfo THEN RS232.Send(tag) END
  END ModuleTag;

  PROCEDURE ModuleTagH*(tag: INTEGER);
  BEGIN
    IF lspMode = LSPhConstants.HighlightCallback THEN
      callback(tag, -1, -1, -1)
    ELSE ModuleTag(tag) END
  END ModuleTagH;

  PROCEDURE ModuleTagPP*(tag, pos: INTEGER);
  BEGIN
    IF lspMode = LSPhConstants.GetModuleInfo THEN RS232.Send(tag); RS232.SendIntRaw(pos) END
  END ModuleTagPP;

  PROCEDURE ModuleTagP*(tag: INTEGER);
  BEGIN ModuleTagPP(tag, Pos())
  END ModuleTagP;

  PROCEDURE FormatTokenTag*(tag: INTEGER);
  BEGIN
    IF lspMode = LSPhConstants.ReFormat THEN RS232.Send(tag)
    ELSIF lspMode = LSPhConstants.FormatCallback THEN callback(tag, -1, -1, -1) END
  END FormatTokenTag;

  PROCEDURE FormatTokenUpdate*(type: INTEGER);
  BEGIN
    IF lspMode = LSPhConstants.ReFormat THEN
      RS232.Send(LSPhConstants.FormatTokenUpdate);
      RS232.Send(type);
    ELSIF lspMode = LSPhConstants.FormatCallback THEN
      callback(LSPhConstants.FormatTokenUpdate, -1, -1, type)
    END
  END FormatTokenUpdate;

  PROCEDURE ClassifyIdent*(syntaxType, defPos: INTEGER; defModName: ARRAY OF CHAR);
  BEGIN
    IF lspMode = LSPhConstants.GetModuleInfo THEN
      ASSERT(lastIdentStart # -1);
      RS232.Send(LSPhConstants.SyntaxElement);
      RS232.SendIntRaw(lastIdentStart);
      RS232.SendIntRaw(Pos());
      RS232.Send(syntaxType);
      IF defPos = -1 THEN
        RS232.SendIntRaw(-1);
      ELSE
        RS232.SendIntRaw(defPos);
        RS232.SendStrRaw(defModName);
      END;
      lastIdentStart := -1
    ELSIF lspMode = LSPhConstants.HighlightCallback THEN
      ASSERT(lastIdentStart # -1);
      callback(LSPhConstants.SyntaxElement,lastIdentStart,Pos(),syntaxType);
      lastIdentStart := -1
    END
  END ClassifyIdent;

  PROCEDURE SymbolFileIndex*(index, defEndPos: INTEGER; defModName: ARRAY OF CHAR);
  BEGIN
    IF lspMode = LSPhConstants.GetModuleInfo THEN
      RS232.Send(LSPhConstants.SymbolFileIndex);
      RS232.SendIntRaw(index);
      RS232.SendStrRaw(defModName);
      RS232.SendIntRaw(defEndPos);
    END
  END SymbolFileIndex;

  PROCEDURE AddCompletion*(syntaxType: INTEGER; suggestion: ARRAY OF CHAR);
    VAR i: INTEGER;
  BEGIN
    IF lspMode = LSPhConstants.AutoComplete THEN
      i := 0; WHILE (suggestion[i] # 0X) & (suggestion[i] = id[i]) DO INC(i) END;
      IF (id[i] = 3X) & (id[i+1] = 0X) THEN
        RS232.Send(LSPhConstants.Completion);
        RS232.Send(syntaxType);
        RS232.SendStrRaw(suggestion);
      END
    END
  END AddCompletion;

  PROCEDURE comment(nested: BOOLEAN);
    VAR begpos: INTEGER;
  BEGIN Texts.Read(R, ch); begpos := Pos() - 2;
    REPEAT
      WHILE ~R.eot & (ch # "*") DO
        IF ch = "(" THEN Texts.Read(R, ch);
          IF ch = "*" THEN comment(TRUE) END
        ELSE Texts.Read(R, ch)
        END
      END ;
      WHILE ch = "*" DO Texts.Read(R, ch) END
    UNTIL (ch = ")") OR R.eot;
    IF ~nested & (lspMode = LSPhConstants.GetModuleInfo) THEN
      RS232.Send(LSPhConstants.SyntaxElement);
      RS232.SendIntRaw(begpos);
      RS232.SendIntRaw(Pos() + 1);
      RS232.Send(LSPhConstants.SynComment);
      RS232.SendIntRaw(-1);
    ELSIF ~nested & (lspMode = LSPhConstants.ReFormat) THEN
      RS232.Send(LSPhConstants.FormatToken);
      RS232.SendIntRaw(begpos);
      RS232.SendIntRaw(Pos() + 1);
      RS232.Send(99);
      RS232.Send(LSPhConstants.TokenIsComment);
    ELSIF ~nested & (lspMode = LSPhConstants.HighlightCallback) THEN
      callback(LSPhConstants.SyntaxElement, begpos, Pos() + 1, LSPhConstants.SynComment);
    ELSIF ~nested & (lspMode = LSPhConstants.FormatCallback) THEN
      callback(LSPhConstants.FormatToken, begpos, Pos() + 1, 99);
      callback(LSPhConstants.TokenIsComment, -1, -1, -1)
    END;
    IF ~R.eot THEN Texts.Read(R, ch) ELSIF ~nested THEN Mark("unterminated comment") END
  END comment;

  PROCEDURE Get*(VAR sym: INTEGER);
    VAR begpos: INTEGER;
  BEGIN
    IF lastIdentStart # -1 THEN
      Mark("LSP: Unclassified identifier");
      lastIdentStart := -1
    END;
    REPEAT
      WHILE ~R.eot & (ch <= " ") & ((lspMode # LSPhConstants.AutoComplete) OR (ch # 3X)) DO Texts.Read(R, ch) END;
      begpos := Pos();
      IF R.eot THEN sym := eot
      ELSIF ch < "A" THEN
        IF ch < "0" THEN
          IF ch = 22X THEN String; sym := string
          ELSIF ch = "#" THEN Texts.Read(R, ch); sym := neq
          ELSIF ch = "$" THEN HexString; sym := string
          ELSIF ch = "&" THEN Texts.Read(R, ch); sym := and
          ELSIF ch = "(" THEN Texts.Read(R, ch); 
            IF ch = "*" THEN sym := null; comment(FALSE) ELSE sym := lparen END
          ELSIF ch = ")" THEN Texts.Read(R, ch); sym := rparen
          ELSIF ch = "*" THEN Texts.Read(R, ch); sym := times
          ELSIF ch = "+" THEN Texts.Read(R, ch); sym := plus
          ELSIF ch = "," THEN Texts.Read(R, ch); sym := comma
          ELSIF ch = "-" THEN Texts.Read(R, ch); sym := minus
          ELSIF ch = "." THEN Texts.Read(R, ch);
            IF ch = "." THEN Texts.Read(R, ch); sym := upto ELSE sym := period END
          ELSIF ch = "/" THEN Texts.Read(R, ch); sym := rdiv
          ELSIF (lspMode = LSPhConstants.AutoComplete) & (ch = 3X) THEN
            Identifier(sym)
          ELSE Texts.Read(R, ch); (* ! % ' *) sym := null
          END
        ELSIF ch < ":" THEN Number(sym)
        ELSIF ch = ":" THEN Texts.Read(R, ch);
          IF ch = "=" THEN Texts.Read(R, ch); sym := becomes ELSE sym := colon END 
        ELSIF ch = ";" THEN Texts.Read(R, ch); sym := semicolon
        ELSIF ch = "<" THEN  Texts.Read(R, ch);
          IF ch = "=" THEN Texts.Read(R, ch); sym := leq ELSE sym := lss END
        ELSIF ch = "=" THEN Texts.Read(R, ch); sym := eql
        ELSIF ch = ">" THEN Texts.Read(R, ch);
          IF ch = "=" THEN Texts.Read(R, ch); sym := geq ELSE sym := gtr END
        ELSE (* ? @ *) Texts.Read(R, ch); sym := null
        END
      ELSIF ch < "[" THEN Identifier(sym)
      ELSIF ch < "a" THEN
        IF ch = "[" THEN sym := lbrak
        ELSIF ch = "]" THEN  sym := rbrak
        ELSIF ch = "^" THEN sym := arrow
        ELSE (* _ ` *) sym := null
        END ;
        Texts.Read(R, ch)
      ELSIF ch < "{" THEN Identifier(sym) ELSE
        IF ch = "{" THEN sym := lbrace
        ELSIF ch = "}" THEN sym := rbrace
        ELSIF ch = "|" THEN sym := bar
        ELSIF ch = "~" THEN  sym := not
        ELSIF ch = 7FX THEN  sym := upto
        ELSE sym := null
        END ;
        Texts.Read(R, ch)
      END
    UNTIL sym # null
    ;IF lspMode = LSPhConstants.GetModuleInfo THEN
      IF sym = ident THEN
        lastIdentStart := begpos;
      ELSIF syntaxType[sym] # 0 THEN
        RS232.Send(LSPhConstants.SyntaxElement);
        RS232.SendIntRaw(begpos);
        RS232.SendIntRaw(Pos());
        RS232.Send(syntaxType[sym]);
        RS232.SendIntRaw(-1);
      END
    ELSIF lspMode = LSPhConstants.HighlightCallback THEN
      IF sym = ident THEN
        lastIdentStart := begpos;
      ELSIF syntaxType[sym] # 0 THEN
        callback(LSPhConstants.SyntaxElement, begpos, Pos(), syntaxType[sym]);
      END
    ELSIF (lspMode = LSPhConstants.ReFormat) & (formatType[sym] # 100) THEN
      RS232.Send(LSPhConstants.FormatToken);
      RS232.SendIntRaw(begpos);
      RS232.SendIntRaw(Pos());
      RS232.Send(formatType[sym]);
    ELSIF (lspMode = LSPhConstants.FormatCallback) & (formatType[sym] # 100) THEN
      callback(LSPhConstants.FormatToken, begpos, Pos(), formatType[sym])
    END;
  END Get;

  PROCEDURE Init*(T: Texts.Text; pos: LONGINT);
  BEGIN errpos := pos - 1; lastIdentStart := -1; errcnt := 0; Texts.OpenReader(R, T, pos); Texts.Read(R, ch)
  END Init;

  PROCEDURE EnterKW(sym: INTEGER; name: ARRAY OF CHAR);
  BEGIN keyTab[k].id := name; keyTab[k].sym := sym; INC(k)
    ;syntaxType[sym] := LSPhConstants.SynKeyword
  END EnterKW;

BEGIN KWX[0] := 0; KWX[1] := 0;
  FOR k := 0 TO eot DO syntaxType[k] := 0; formatType[k] := 11 END;
  formatType[null] := 100;
  formatType[eot] := 100;
  formatType[arrow] := 08;
  formatType[period] := 00;
  formatType[char] := 99;
  formatType[int] := 99;
  formatType[real] := 99;
  formatType[false] := 99;
  formatType[true] := 99;
  formatType[nil] := 99;
  formatType[string] := 99;
  formatType[not] := 10;
  formatType[lparen] := 90;
  formatType[lbrak] := 00;
  formatType[lbrace] := 10;
  formatType[ident] := 99;
  formatType[comma] := 01;
  formatType[colon] := 01;
  formatType[rparen] := 09;
  formatType[rbrak] := 09;
  formatType[rbrace] := 01;
  formatType[semicolon] := 01;
  formatType[const] := 21;
  formatType[type] := 21;
  formatType[begin] := 21;
  formatType[var] := 91;
  k := 0; lastIdentStart := -1;
  EnterKW(if, "IF");
  EnterKW(do, "DO");
  EnterKW(of, "OF");
  EnterKW(or, "OR");
  EnterKW(to, "TO");
  EnterKW(in, "IN");
  EnterKW(is, "IS");
  EnterKW(by, "BY");
  KWX[2] := k;
  EnterKW(end, "END");
  EnterKW(nil, "NIL");
  EnterKW(var, "VAR");
  EnterKW(div, "DIV");
  EnterKW(mod, "MOD");
  EnterKW(for, "FOR");
  KWX[3] := k;
  EnterKW(else, "ELSE");
  EnterKW(then, "THEN");
  EnterKW(true, "TRUE");
  EnterKW(type, "TYPE");
  EnterKW(case, "CASE");
  KWX[4] := k;
  EnterKW(elsif, "ELSIF");
  EnterKW(false, "FALSE");
  EnterKW(array, "ARRAY");
  EnterKW(begin, "BEGIN");
  EnterKW(const, "CONST");
  EnterKW(until, "UNTIL");
  EnterKW(while, "WHILE");
  KWX[5] := k;
  EnterKW(record, "RECORD");
  EnterKW(repeat, "REPEAT");
  EnterKW(return, "RETURN");
  EnterKW(import, "IMPORT");
  EnterKW(module, "MODULE");
  KWX[6] := k;
  EnterKW(pointer, "POINTER");
  KWX[7] := k; KWX[8] := k;
  syntaxType[times] := LSPhConstants.SynOperator;
  syntaxType[rdiv] := LSPhConstants.SynOperator;
  syntaxType[div] := LSPhConstants.SynOperator;
  syntaxType[mod] := LSPhConstants.SynOperator;
  syntaxType[and] := LSPhConstants.SynOperator;
  syntaxType[plus] := LSPhConstants.SynOperator;
  syntaxType[minus] := LSPhConstants.SynOperator;
  syntaxType[or] := LSPhConstants.SynOperator;
  syntaxType[eql] := LSPhConstants.SynOperator;
  syntaxType[neq] := LSPhConstants.SynOperator;
  syntaxType[lss] := LSPhConstants.SynOperator;
  syntaxType[leq] := LSPhConstants.SynOperator;
  syntaxType[gtr] := LSPhConstants.SynOperator;
  syntaxType[geq] := LSPhConstants.SynOperator;
  syntaxType[in] := LSPhConstants.SynOperator;
  syntaxType[is] := LSPhConstants.SynOperator;
  syntaxType[arrow] := LSPhConstants.SynOperator;
  syntaxType[period] := LSPhConstants.SynOperator;
  syntaxType[char] := LSPhConstants.SynType;
  syntaxType[int] := LSPhConstants.SynType;
  syntaxType[real] := LSPhConstants.SynType;
  syntaxType[false] := LSPhConstants.SynConstant;
  syntaxType[true] := LSPhConstants.SynConstant;
  syntaxType[nil] := LSPhConstants.SynConstant;
  syntaxType[string] := LSPhConstants.SynString;
  syntaxType[not] := LSPhConstants.SynOperator;
  syntaxType[upto] := LSPhConstants.SynOperator;
  EnterKW(procedure, "PROCEDURE");
  KWX[9] := k
END LSPhORS.
