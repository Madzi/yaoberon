package io.github.madzi.yaoberon.lexer;

import io.github.madzi.yaoberon.lexer.token.Token;
import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.lexer.token.Trivia;
import io.github.madzi.yaoberon.source.SourceFile;
import io.github.madzi.yaoberon.source.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LexerTest {

    private final Lexer lexer = Lexer.create();

    private SourceFile source(String content) {
        return SourceFile.create("test://code.ob", 1, content);
    }

    private List<Token> tokenize(String content) {
        return lexer.tokenizeAll(source(content)).tokens();
    }

    @Test
    void testCreate() {
        assertNotNull(lexer);
    }

    @Test
    void testEmptySource() {
        ScanResult result = lexer.tokenizeAll(source(""));
        assertEquals(1, result.tokens().size());
        Token eot = result.tokens().getFirst();
        assertEquals(TokenKind.EOT, eot.kind());
        assertTrue(eot.isEot());
        assertEquals(new Span(0, 0), eot.span());
        assertEquals(0, result.stoppedAtOffset());
        assertEquals(LexerState.INITIAL, result.finalState());
    }

    @Test
    void testAllKeywords() {
        String[] keywords = {
            "IF", "DO", "OF", "OR", "TO", "IN", "IS", "BY", "END", "NIL", "VAR",
            "DIV", "MOD", "FOR", "ELSE", "THEN", "TRUE", "TYPE", "CASE", "ELSIF",
            "FALSE", "ARRAY", "BEGIN", "CONST", "UNTIL", "WHILE", "RECORD", "REPEAT",
            "RETURN", "IMPORT", "MODULE", "POINTER", "PROCEDURE"
        };
        for (String kw : keywords) {
            List<Token> tokens = tokenize(kw);
            assertEquals(2, tokens.size(), "Should produce keyword and EOT for: " + kw);
            assertEquals(TokenKind.valueOf(kw), tokens.get(0).kind());
            assertEquals(new Span(0, kw.length()), tokens.get(0).span());
        }
    }

    @Test
    void testIdentifiers() {
        String text = "x abc MyVar foo123 A_B_C module_name";
        List<Token> tokens = tokenize(text);
        assertEquals(7, tokens.size()); // 6 idents + EOT
        for (int i = 0; i < 6; i++) {
            assertEquals(TokenKind.IDENT, tokens.get(i).kind());
        }
    }

    @Test
    void testIdentifierCannotStartWithUnderscore() {
        List<Token> tokens = tokenize("_ident");
        assertEquals(3, tokens.size()); // ERROR, IDENT, EOT
        assertEquals(TokenKind.ERROR, tokens.get(0).kind());
        assertEquals(TokenKind.IDENT, tokens.get(1).kind());
        assertEquals(TokenKind.EOT, tokens.get(2).kind());
    }

    @Test
    void testMixedCaseKeywordsAreIdentifiers() {
        List<Token> tokens = tokenize("module Module if If True False nil");
        assertEquals(8, tokens.size()); // 7 tokens + EOT
        for (int i = 0; i < 7; i++) {
            assertEquals(TokenKind.IDENT, tokens.get(i).kind());
        }
    }

    @Test
    void testIntegersDecimal() {
        List<Token> tokens = tokenize("0 42 123456789");
        assertEquals(4, tokens.size());
        assertEquals(TokenKind.INT, tokens.get(0).kind());
        assertEquals(TokenKind.INT, tokens.get(1).kind());
        assertEquals(TokenKind.INT, tokens.get(2).kind());
        assertEquals(TokenKind.EOT, tokens.get(3).kind());
    }

    @Test
    void testIntegersHexadecimal() {
        List<Token> tokens = tokenize("0H 0AFH 123h 0ffffh");
        assertEquals(5, tokens.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(TokenKind.INT, tokens.get(i).kind());
        }
    }

    @Test
    void testRealNumbers() {
        List<Token> tokens = tokenize("1.0 0.5 3.14159 123R 456r");
        assertEquals(6, tokens.size());
        for (int i = 0; i < 5; i++) {
            assertEquals(TokenKind.REAL, tokens.get(i).kind());
        }
    }

    @Test
    void testRealNumbersWithExponent() {
        List<Token> tokens = tokenize("1.2E3 1.2e3 1.2E+3 1.2e-3 2.5D4 2.5d4 2.5D+4 2.5d-4");
        assertEquals(9, tokens.size());
        for (int i = 0; i < 8; i++) {
            assertEquals(TokenKind.REAL, tokens.get(i).kind());
        }
    }

    @Test
    void testIntegerFollowedByUpTo() {
        List<Token> tokens = tokenize("1..10");
        assertEquals(4, tokens.size()); // INT, UPTO, INT, EOT
        assertEquals(TokenKind.INT, tokens.get(0).kind());
        assertEquals(new Span(0, 1), tokens.get(0).span());
        assertEquals(TokenKind.UPTO, tokens.get(1).kind());
        assertEquals(new Span(1, 2), tokens.get(1).span());
        assertEquals(TokenKind.INT, tokens.get(2).kind());
        assertEquals(new Span(3, 2), tokens.get(2).span());
    }

    @Test
    void testHexCharacterConstants() {
        List<Token> tokens = tokenize("0X 0AX 41x 0FFX");
        assertEquals(5, tokens.size());
        for (int i = 0; i < 4; i++) {
            assertEquals(TokenKind.CHAR, tokens.get(i).kind());
        }
    }

    @Test
    void testDoubleQuotedStringsAndChars() {
        List<Token> tokens = tokenize("\"Hello, World!\" \"\" \"A\"");
        assertEquals(4, tokens.size());
        assertEquals(TokenKind.STRING, tokens.get(0).kind());
        assertEquals(TokenKind.STRING, tokens.get(1).kind());
        assertEquals(TokenKind.CHAR, tokens.get(2).kind());
        assertEquals(TokenKind.EOT, tokens.get(3).kind());
    }

    @Test
    void testUnclosedDoubleQuotedStrings() {
        List<Token> tokens = tokenize("\"unclosed string\n");
        assertEquals(2, tokens.size());
        assertEquals(TokenKind.STRING, tokens.get(0).kind());

        List<Token> tokens2 = tokenize("\"unclosed string\r");
        assertEquals(2, tokens2.size());
        assertEquals(TokenKind.STRING, tokens2.get(0).kind());

        List<Token> tokens3 = tokenize("\"unclosed at eof");
        assertEquals(2, tokens3.size());
        assertEquals(TokenKind.STRING, tokens3.get(0).kind());

        List<Token> tokens4 = tokenize("\"A");
        assertEquals(2, tokens4.size());
        assertEquals(TokenKind.CHAR, tokens4.get(0).kind());
    }

    @Test
    void testSingleQuotedStringsAndChars() {
        List<Token> tokens = tokenize("'Hello, World!' '' 'A'");
        assertEquals(4, tokens.size());
        assertEquals(TokenKind.STRING, tokens.get(0).kind());
        assertEquals(TokenKind.STRING, tokens.get(1).kind());
        assertEquals(TokenKind.CHAR, tokens.get(2).kind());
    }

    @Test
    void testUnclosedSingleQuotedStrings() {
        List<Token> tokens = tokenize("'unclosed single\n");
        assertEquals(2, tokens.size());
        assertEquals(TokenKind.STRING, tokens.get(0).kind());

        List<Token> tokens2 = tokenize("'unclosed single\r");
        assertEquals(2, tokens2.size());
        assertEquals(TokenKind.STRING, tokens2.get(0).kind());

        List<Token> tokens3 = tokenize("'unclosed at eof");
        assertEquals(2, tokens3.size());
        assertEquals(TokenKind.STRING, tokens3.get(0).kind());

        List<Token> tokens4 = tokenize("'z");
        assertEquals(2, tokens4.size());
        assertEquals(TokenKind.CHAR, tokens4.get(0).kind());
    }

    @Test
    void testDollarStringsAndChars() {
        List<Token> tokens = tokenize("$dollar string$ $$ $A$");
        assertEquals(4, tokens.size());
        assertEquals(TokenKind.STRING, tokens.get(0).kind());
        assertEquals(TokenKind.STRING, tokens.get(1).kind());
        assertEquals(TokenKind.CHAR, tokens.get(2).kind());

        List<Token> tokensUnclosed = tokenize("$unclosed dollar");
        assertEquals(2, tokensUnclosed.size());
        assertEquals(TokenKind.STRING, tokensUnclosed.get(0).kind());

        List<Token> tokensUnclosedChar = tokenize("$X");
        assertEquals(2, tokensUnclosedChar.size());
        assertEquals(TokenKind.CHAR, tokensUnclosedChar.get(0).kind());
    }

    @Test
    void testOperatorsAndPunctuation() {
        String text = "= # < <= > >= ; . .. : := , + - * / ( ) [ ] { } ^ ↑ ~ & |";
        List<Token> tokens = tokenize(text);
        TokenKind[] expectedKinds = {
            TokenKind.EQL, TokenKind.NEQ, TokenKind.LSS, TokenKind.LEQ, TokenKind.GTR, TokenKind.GEQ,
            TokenKind.SEMICOLON, TokenKind.PERIOD, TokenKind.UPTO, TokenKind.COLON, TokenKind.BECOMES,
            TokenKind.COMMA, TokenKind.PLUS, TokenKind.MINUS, TokenKind.TIMES, TokenKind.RDIV,
            TokenKind.LPAREN, TokenKind.RPAREN, TokenKind.LBRAK, TokenKind.RBRAK,
            TokenKind.LBRACE, TokenKind.RBRACE, TokenKind.ARROW, TokenKind.ARROW,
            TokenKind.NOT, TokenKind.AND, TokenKind.BAR, TokenKind.EOT
        };

        assertEquals(expectedKinds.length, tokens.size());
        for (int i = 0; i < expectedKinds.length; i++) {
            assertEquals(expectedKinds[i], tokens.get(i).kind(), "Mismatch at index " + i);
        }
    }

    @Test
    void testSingleCharUpto7FX() {
        List<Token> tokens = tokenize("\u007F");
        assertEquals(2, tokens.size()); // UPTO, EOT
        assertEquals(TokenKind.UPTO, tokens.get(0).kind());
        assertEquals(new Span(0, 1), tokens.get(0).span());
    }

    @Test
    void testErrorToken() {
        List<Token> tokens = tokenize("@ ? `");
        assertEquals(4, tokens.size());
        assertEquals(TokenKind.ERROR, tokens.get(0).kind());
        assertEquals(TokenKind.ERROR, tokens.get(1).kind());
        assertEquals(TokenKind.ERROR, tokens.get(2).kind());
        assertEquals(TokenKind.EOT, tokens.get(3).kind());
    }

    @Test
    void testWhitespaceTrivia() {
        List<Token> tokens = tokenize("   \t\r\n   A");
        assertEquals(2, tokens.size());
        Token tokenA = tokens.get(0);
        assertEquals(TokenKind.IDENT, tokenA.kind());
        assertEquals(1, tokenA.trivias().size());
        assertInstanceOf(Trivia.Whitespace.class, tokenA.trivias().get(0));
    }

    @Test
    void testTrailingWhitespaceTrivia() {
        List<Token> tokens = tokenize("A   ");
        assertEquals(2, tokens.size());
        Token eot = tokens.get(1);
        assertEquals(TokenKind.EOT, eot.kind());
        assertEquals(1, eot.trivias().size());
        assertInstanceOf(Trivia.Whitespace.class, eot.trivias().get(0));
    }

    @Test
    void testCommentsSingleAndNested() {
        String text = "(* simple comment *)\n" +
                      "(* outer (* inner *) still outer *)\n" +
                      "VAR (* trailing comment *)";
        List<Token> tokens = tokenize(text);
        assertEquals(2, tokens.size()); // VAR, EOT

        Token varToken = tokens.get(0);
        assertEquals(TokenKind.VAR, varToken.kind());
        assertEquals(4, varToken.trivias().size()); // comment, ws, comment, ws

        Trivia c1 = varToken.trivias().get(0);
        assertInstanceOf(Trivia.Comment.class, c1);
        assertTrue(((Trivia.Comment) c1).isNestedCorrectly());

        Trivia c2 = varToken.trivias().get(2);
        assertInstanceOf(Trivia.Comment.class, c2);
        assertTrue(((Trivia.Comment) c2).isNestedCorrectly());

        Token eot = tokens.get(1);
        assertEquals(TokenKind.EOT, eot.kind());
        assertEquals(2, eot.trivias().size()); // ws, comment
        assertInstanceOf(Trivia.Comment.class, eot.trivias().get(1));
    }

    @Test
    void testUnclosedComment() {
        String text = "(* unclosed comment";
        ScanResult result = lexer.tokenizeAll(source(text));
        assertEquals(1, result.tokens().size()); // EOT
        Token eot = result.tokens().getFirst();
        assertEquals(1, eot.trivias().size());
        Trivia trivia = eot.trivias().getFirst();
        assertInstanceOf(Trivia.Comment.class, trivia);
        assertFalse(((Trivia.Comment) trivia).isNestedCorrectly());
        assertEquals(1, result.finalState().commentNestingDepth());
    }

    @Test
    void testCommentWithSpecialCharacters() {
        String text = "(* test * and ( and ** not closed here *) A";
        List<Token> tokens = tokenize(text);
        assertEquals(2, tokens.size());
        assertEquals(TokenKind.IDENT, tokens.get(0).kind());
    }

    @Test
    void testTokenizeRange() {
        SourceFile src = source("MODULE Test; (* comment *) BEGIN END Test.");
        // Range just covering "Test;"
        ScanResult result = lexer.tokenizeRange(src, 7, 12, LexerState.INITIAL);
        assertEquals(3, result.tokens().size()); // Test, ;, EOT
        assertEquals(TokenKind.IDENT, result.tokens().get(0).kind());
        assertEquals(TokenKind.SEMICOLON, result.tokens().get(1).kind());
        assertEquals(TokenKind.EOT, result.tokens().get(2).kind());
        assertEquals(12, result.stoppedAtOffset());
    }

    @Test
    void testTokenizeRangeWithInitialCommentDepth() {
        SourceFile src = source("inside comment *) A");
        LexerState state = new LexerState(1);
        ScanResult result = lexer.tokenizeRange(src, 0, src.length(), state);
        assertEquals(2, result.tokens().size()); // A, EOT
        assertEquals(TokenKind.IDENT, result.tokens().get(0).kind());
        assertEquals(LexerState.INITIAL, result.finalState());
        assertEquals(2, result.tokens().get(0).trivias().size());
        assertInstanceOf(Trivia.Comment.class, result.tokens().get(0).trivias().get(0));
    }

    @Test
    void testTokenizeRangeWithInitialCommentDepthUnclosed() {
        SourceFile src = source("still inside comment without closing");
        LexerState state = new LexerState(2);
        ScanResult result = lexer.tokenizeRange(src, 0, src.length(), state);
        assertEquals(1, result.tokens().size()); // EOT
        assertEquals(2, result.finalState().commentNestingDepth());
    }

    @Test
    void testTokenizeRangeEndOffsetBeyondLength() {
        SourceFile src = source("x");
        ScanResult result = lexer.tokenizeRange(src, 0, 100, LexerState.INITIAL);
        assertEquals(2, result.tokens().size()); // x, EOT
        assertEquals(1, result.stoppedAtOffset());
    }
}
