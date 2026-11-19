package io.github.madzi.yaoberon.parser;

import io.github.madzi.yaoberon.lexer.Lexer;
import io.github.madzi.yaoberon.lexer.ScanResult;
import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import io.github.madzi.yaoberon.parser.internal.BaseParser;
import io.github.madzi.yaoberon.parser.internal.DetailedParser;
import io.github.madzi.yaoberon.source.SourceFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ParserTest {

    private final Lexer lexer = Lexer.create();
    private final Parser parser = Parser.create();
    private final DetailedParser detailedParser = new BaseParser();

    private ParseContext context(String text) {
        SourceFile source = SourceFile.create("test://code.ob", 1, text);
        ScanResult scan = lexer.tokenizeAll(source);
        return new ParseContext(source, scan.tokens());
    }

    private ParseResult parse(String text) {
        return parser.parse(context(text));
    }

    @Test
    void testParserCreate() {
        assertNotNull(parser);
    }

    @Test
    void testModuleWithEmptyBegin() {
        String code = "MODULE EmptyBegin; BEGIN END EmptyBegin.";
        ParseResult result = parse(code);
        assertEquals(SyntaxKind.MODULE_DECLARATION, result.root().kind());
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testEmptyIfAndLoopBranches() {
        String code = """
            MODULE EmptyBranches;
            BEGIN
                IF x > 0 THEN END;
                IF a > 0 THEN ELSIF b > 0 THEN ELSE END;
                WHILE x > 0 DO END;
                REPEAT UNTIL x = 0
            END EmptyBranches.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testForStatementWithByStep() {
        String code = """
            MODULE ForBy;
            BEGIN
                FOR i := 1 TO 10 BY 2 DO
                    x := x + i
                END;
                FOR i := 10 TO 1 BY -1 DO
                    x := x - i
                END
            END ForBy.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testCaseStatementWithRange() {
        String code = """
            MODULE CaseRange;
            BEGIN
                CASE ch OF
                    "A".."Z": y := 1
                  | 1..10, 20..30: y := 2
                  | id: y := 3
                END
            END CaseRange.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testTypeAliases() {
        String code = """
            MODULE TypeAliases;
            TYPE
                IntAlias = INTEGER;
                QualType = Out.Stream;
                ArrAlias = ARRAY 10 OF INTEGER;
            END TypeAliases.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testModuleWithBegin() {
        String code = "MODULE WithBegin; BEGIN x := 1 END WithBegin.";
        ParseResult result = parse(code);
        assertEquals(SyntaxKind.MODULE_DECLARATION, result.root().kind());
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testModuleWithImports() {
        String code = "MODULE WithImports; IMPORT Out, M := Math, In; BEGIN x := 1 END WithImports.";
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testConstDeclarations() {
        String code = """
            MODULE Consts;
            CONST
                c1 = 10;
                c2* = 20 + 5;
                c3 = "hello";
                c4 = 3.14;
            END Consts.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testTypeDeclarations() {
        String code = """
            MODULE Types;
            TYPE
                Vector = ARRAY 10 OF INTEGER;
                Matrix = ARRAY 5, 10 OF REAL;
                BaseRec = RECORD END;
                ExtRec = RECORD (BaseRec)
                    x, y*: INTEGER;
                    name: ARRAY 32 OF CHAR
                END;
                NodePtr = POINTER TO ExtRec;
                ProcType1 = PROCEDURE;
                ProcType2 = PROCEDURE (CONST a: INTEGER; VAR b: REAL): BOOLEAN;
            END Types.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testVarDeclarations() {
        String code = """
            MODULE Vars;
            VAR
                a, b*: INTEGER;
                flag: BOOLEAN;
                data: ARRAY 100 OF BYTE;
            END Vars.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testProcedureDeclarations() {
        String code = """
            MODULE Procs;
            VAR global: INTEGER;

            PROCEDURE Simple;
            BEGIN
                global := 1
            END Simple;

            PROCEDURE Add*(CONST a: INTEGER; VAR b: INTEGER): INTEGER;
            BEGIN
                RETURN a + b
            END Add;

            PROCEDURE Process(arr: ARRAY OF INTEGER);
            CONST max = 100;
            VAR local: INTEGER;
                PROCEDURE Nested;
                BEGIN
                    local := 0
                END Nested;
            BEGIN
                local := arr[0];
                Nested
            END Process;

            PROCEDURE ReturnOnly(): INTEGER;
            BEGIN
                RETURN 42
            END ReturnOnly;

            PROCEDURE EmptyBody;
            CONST c = 1;
            END EmptyBody;

            END Procs.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testAssignmentsAndProcedureCalls() {
        String code = """
            MODULE Statements;
            VAR x, y: INTEGER;
                arr: ARRAY 10 OF INTEGER;
                rec: RECORD field: INTEGER END;
                ptr: POINTER TO RECORD val: INTEGER END;
            BEGIN
                x := 10;
                y := x + 5;
                rec.field := 20;
                arr[0] := 30;
                ptr^.val := 40;
                Out.Int(x, 0);
                Out.Ln;
                DoAction()
            END Statements.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testIfStatement() {
        String code = """
            MODULE IfTest;
            BEGIN
                IF x > 0 THEN
                    y := 1
                ELSIF x < 0 THEN
                    y := -1
                ELSIF x = 0 THEN
                    y := 0
                ELSE
                    y := 99
                END;
                IF single THEN y := 2 END
            END IfTest.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testCaseStatement() {
        String code = """
            MODULE CaseTest;
            BEGIN
                CASE ch OF
                    "A": y := 1
                  | "B": y := 2
                  | 10: y := 3
                  | id: y := 4
                END
            END CaseTest.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testWhileStatement() {
        String code = """
            MODULE WhileTest;
            BEGIN
                WHILE x > 0 DO
                    x := x - 1
                ELSIF y > 0 DO
                    y := y - 1
                END
            END WhileTest.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testRepeatStatement() {
        String code = """
            MODULE RepeatTest;
            BEGIN
                REPEAT
                    x := x + 1;
                    y := y - 1
                UNTIL (x >= 10) OR (y <= 0)
            END RepeatTest.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testForStatement() {
        String code = """
            MODULE ForTest;
            BEGIN
                FOR i := 1 TO 10 DO
                    sum := sum + i
                END
            END ForTest.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testExpressionsAndRelations() {
        String code = """
            MODULE ExprTest;
            BEGIN
                b := (x = y) OR (x # y) OR (x < y) OR (x <= y) OR (x > y) OR (x >= y);
                b := (x IN s) OR (x IS MyType);
                n := +a - b;
                m := a * b / c DIV d MOD e & f;
                val := NIL;
                t := TRUE;
                f := FALSE;
                s1 := {};
                s2 := {1, 2, 8};
                neg := ~done;
                call := Func(1, 2 + 3);
                indexed := arr[i, j][k].f^
            END ExprTest.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testStatementSequenceWithExtraSemicolons() {
        String code = """
            MODULE Semicolons;
            BEGIN
                ; x := 1; ; y := 2; ;
            END Semicolons.
            """;
        ParseResult result = parse(code);
        assertTrue(result.diagnostics().isEmpty(), "Errors: " + result.diagnostics());
    }

    @Test
    void testDetailedParserParseIdent() {
        ParseContext ctxValid = context("myIdent");
        GreenNode valid = detailedParser.parseIdent(ctxValid);
        assertEquals(SyntaxKind.IDENTIFIER, valid.kind());
        assertTrue(ctxValid.diagnostics().isEmpty());

        ParseContext ctxInvalid = context("123");
        GreenNode invalid = detailedParser.parseIdent(ctxInvalid);
        assertEquals(SyntaxKind.ERROR, invalid.kind());
        assertEquals(1, ctxInvalid.diagnostics().size());
        assertEquals("Expected identifier", ctxInvalid.diagnostics().get(0).message());
    }

    @Test
    void testDetailedParserParseNumber() {
        ParseContext ctxInt = context("42");
        assertEquals(SyntaxKind.INT_LITERAL, detailedParser.parseNumber(ctxInt).kind());

        ParseContext ctxReal = context("3.14");
        assertEquals(SyntaxKind.REAL_LITERAL, detailedParser.parseNumber(ctxReal).kind());

        ParseContext ctxInvalid = context("\"str\"");
        assertEquals(SyntaxKind.ERROR, detailedParser.parseNumber(ctxInvalid).kind());
        assertEquals(1, ctxInvalid.diagnostics().size());
        assertEquals("Expected number (integer or real)", ctxInvalid.diagnostics().get(0).message());
    }

    @Test
    void testDetailedParserParseStrucType() {
        ParseContext ctxArr = context("ARRAY 5 OF INTEGER");
        assertEquals(SyntaxKind.ARRAY_TYPE, detailedParser.parseStrucType(ctxArr).kind());

        ParseContext ctxRec = context("RECORD END");
        assertEquals(SyntaxKind.RECORD_TYPE, detailedParser.parseStrucType(ctxRec).kind());

        ParseContext ctxPtr = context("POINTER TO T");
        assertEquals(SyntaxKind.POINTER_TYPE, detailedParser.parseStrucType(ctxPtr).kind());

        ParseContext ctxProc = context("PROCEDURE");
        assertEquals(SyntaxKind.PROCEDURE_TYPE, detailedParser.parseStrucType(ctxProc).kind());

        ParseContext ctxInvalid = context("123");
        assertEquals(SyntaxKind.ERROR, detailedParser.parseStrucType(ctxInvalid).kind());
        assertEquals(1, ctxInvalid.diagnostics().size());
    }

    @Test
    void testDetailedParserParseType() {
        ParseContext ctxIdent = context("MyType");
        assertEquals(SyntaxKind.QUALIDENT, detailedParser.parseType(ctxIdent).kind());

        ParseContext ctxStruc = context("ARRAY 10 OF CHAR");
        assertEquals(SyntaxKind.ARRAY_TYPE, detailedParser.parseType(ctxStruc).kind());

        ParseContext ctxInvalid = context("123");
        assertEquals(SyntaxKind.ERROR, detailedParser.parseType(ctxInvalid).kind());
        assertEquals(1, ctxInvalid.diagnostics().size());
    }

    @Test
    void testDetailedParserParseRelation() {
        for (String rel : new String[]{"=", "#", "<", "<=", ">", ">=", "IN", "IS"}) {
            ParseContext ctx = context(rel);
            assertEquals(SyntaxKind.OPERATOR, detailedParser.parseRelation(ctx).kind());
        }

        ParseContext ctxInvalid = context("+");
        assertEquals(SyntaxKind.ERROR, detailedParser.parseRelation(ctxInvalid).kind());
        assertEquals(1, ctxInvalid.diagnostics().size());
    }

    @Test
    void testDetailedParserParseAddOp() {
        for (String op : new String[]{"+", "-", "OR"}) {
            ParseContext ctx = context(op);
            assertEquals(SyntaxKind.OPERATOR, detailedParser.parseAddOp(ctx).kind());
        }

        ParseContext ctxInvalid = context("*");
        assertEquals(SyntaxKind.ERROR, detailedParser.parseAddOp(ctxInvalid).kind());
        assertEquals(1, ctxInvalid.diagnostics().size());
    }

    @Test
    void testDetailedParserParseMulOperator() {
        for (String op : new String[]{"*", "/", "DIV", "MOD", "&"}) {
            ParseContext ctx = context(op);
            assertEquals(SyntaxKind.OPERATOR, detailedParser.parseMulOperator(ctx).kind());
        }

        ParseContext ctxInvalid = context("+");
        assertEquals(SyntaxKind.ERROR, detailedParser.parseMulOperator(ctxInvalid).kind());
        assertEquals(1, ctxInvalid.diagnostics().size());
    }

    @Test
    void testDetailedParserParseFactorError() {
        ParseContext ctxInvalid = context(";");
        assertEquals(SyntaxKind.ERROR, detailedParser.parseFactor(ctxInvalid).kind());
        assertEquals(1, ctxInvalid.diagnostics().size());
    }

    @Test
    void testDetailedParserParseLabel() {
        ParseContext ctxInt = context("123");
        assertEquals(SyntaxKind.INT_LITERAL, detailedParser.parseLabel(ctxInt).kind());

        ParseContext ctxStr = context("\"hello\"");
        assertEquals(SyntaxKind.STRING_LITERAL, detailedParser.parseLabel(ctxStr).kind());

        ParseContext ctxIdent = context("foo");
        assertEquals(SyntaxKind.IDENTIFIER, detailedParser.parseLabel(ctxIdent).kind());

        ParseContext ctxInvalid = context(";");
        assertEquals(SyntaxKind.ERROR, detailedParser.parseLabel(ctxInvalid).kind());
        assertEquals(1, ctxInvalid.diagnostics().size());
    }

    @Test
    void testSyntaxErrorsInModule() {
        // Missing module name and punctuation
        ParseResult r1 = parse("NOT_MODULE; END.");
        assertFalse(r1.diagnostics().isEmpty());

        // Missing '=' in CONST
        ParseResult r2 = parse("MODULE M; CONST a 1; END M.");
        assertFalse(r2.diagnostics().isEmpty());

        // Missing '=' in TYPE
        ParseResult r3 = parse("MODULE M; TYPE T INTEGER; END M.");
        assertFalse(r3.diagnostics().isEmpty());

        // Invalid type in TYPE declaration
        ParseResult r3b = parse("MODULE M; TYPE T = 123; END M.");
        assertFalse(r3b.diagnostics().isEmpty());

        // Missing 'OF' in ARRAY
        ParseResult r4 = parse("MODULE M; TYPE A = ARRAY 10 INTEGER; END M.");
        assertFalse(r4.diagnostics().isEmpty());

        // Missing ')' in RECORD
        ParseResult r5 = parse("MODULE M; TYPE R = RECORD (Base f: INTEGER END; END M.");
        assertFalse(r5.diagnostics().isEmpty());

        // Missing ':' in field list
        ParseResult r6 = parse("MODULE M; TYPE R = RECORD f INTEGER END; END M.");
        assertFalse(r6.diagnostics().isEmpty());

        // Missing ':=' in assignment
        ParseResult r7 = parse("MODULE M; BEGIN a 1 END M.");
        assertFalse(r7.diagnostics().isEmpty());

        // Missing ':' in CASE
        ParseResult r8 = parse("MODULE M; BEGIN CASE x OF 1 y := 1 END END M.");
        assertFalse(r8.diagnostics().isEmpty());

        // Missing ':=' in FOR
        ParseResult r9 = parse("MODULE M; BEGIN FOR i 1 TO 10 DO END END M.");
        assertFalse(r9.diagnostics().isEmpty());

        // Missing 'TO' in FOR
        ParseResult r10 = parse("MODULE M; BEGIN FOR i := 1 10 DO END END M.");
        assertFalse(r10.diagnostics().isEmpty());

        // Missing ';' in procedure heading
        ParseResult r11 = parse("MODULE M; PROCEDURE P BEGIN END P; END M.");
        assertFalse(r11.diagnostics().isEmpty());

        // Missing '(' in formal parameters
        ParseResult r12 = parse("MODULE M; PROCEDURE P ) ; BEGIN END P; END M.");
        assertFalse(r12.diagnostics().isEmpty());

        // Missing 'OF' in formal type
        ParseResult r13 = parse("MODULE M; PROCEDURE P(arr: ARRAY INTEGER); BEGIN END P; END M.");
        assertFalse(r13.diagnostics().isEmpty());

        // Missing final '.' in module
        ParseResult r14 = parse("MODULE M; END M");
        assertFalse(r14.diagnostics().isEmpty());
    }
}
