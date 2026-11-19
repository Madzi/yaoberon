package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.lexer.Lexer;
import io.github.madzi.yaoberon.lexer.ScanResult;
import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.parser.ParseContext;
import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import io.github.madzi.yaoberon.source.SourceFile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GreenNodeBuilderTest {

    private final Lexer lexer = Lexer.create();

    private ParseContext context(String text) {
        SourceFile source = SourceFile.create("test://code", 1, text);
        ScanResult scan = lexer.tokenizeAll(source);
        return new ParseContext(source, scan.tokens());
    }

    @Test
    void testSeparatedSequence() {
        ParseContext ctx = context("A, B, C;");
        GreenNodeBuilder builder = new GreenNodeBuilder(ctx);

        builder.separatedSequence(
            TokenKind.IDENT,
            TokenKind.COMMA,
            SyntaxKind.PUNCTUATION,
            c -> c.consume(SyntaxKind.IDENTIFIER)
        );

        GreenNode node = builder.node(SyntaxKind.IDENTIFIER_LIST);
        assertInstanceOf(GreenNode.Composite.class, node);
        GreenNode.Composite comp = (GreenNode.Composite) node;
        // Should have: IDENT(A), PUNCTUATION(,), IDENT(B), PUNCTUATION(,), IDENT(C) = 5 children
        assertEquals(5, comp.children().size());
    }

    @Test
    void testSeparatedSequenceTrailingSeparator() {
        ParseContext ctx = context("A, ;");
        GreenNodeBuilder builder = new GreenNodeBuilder(ctx);

        builder.separatedSequence(
            TokenKind.IDENT,
            TokenKind.COMMA,
            SyntaxKind.PUNCTUATION,
            c -> c.consume(SyntaxKind.IDENTIFIER)
        );

        GreenNode.Composite comp = (GreenNode.Composite) builder.node(SyntaxKind.IDENTIFIER_LIST);
        // A, PUNCTUATION(,) -> stopped because ';' is not IDENT
        assertEquals(2, comp.children().size());
    }

    @Test
    void testSequence() {
        ParseContext ctx = context("1 2 3 ;");
        GreenNodeBuilder builder = new GreenNodeBuilder(ctx);

        builder.sequence(TokenKind.INT, c -> c.consume(SyntaxKind.INT_LITERAL));

        GreenNode.Composite comp = (GreenNode.Composite) builder.node(SyntaxKind.STATEMENT_SEQUENCE);
        assertEquals(3, comp.children().size());
    }

    @Test
    void testOptionalRule() {
        ParseContext ctx = context("* x");
        GreenNodeBuilder builder = new GreenNodeBuilder(ctx);
        builder.optional(TokenKind.TIMES, c -> c.consume(SyntaxKind.OPERATOR));
        GreenNode.Composite comp = (GreenNode.Composite) builder.node(SyntaxKind.IDENTIFIER_DEFINITION);
        assertEquals(1, comp.children().size());

        // Optional when token does not match
        ParseContext ctx2 = context("x");
        GreenNodeBuilder builder2 = new GreenNodeBuilder(ctx2);
        builder2.optional(TokenKind.TIMES, c -> c.consume(SyntaxKind.OPERATOR));
        GreenNode.Composite comp2 = (GreenNode.Composite) builder2.node(SyntaxKind.IDENTIFIER_DEFINITION);
        assertTrue(comp2.children().isEmpty());
    }

    @Test
    void testOptionalSyntaxKind() {
        ParseContext ctx = context("* x");
        GreenNodeBuilder builder = new GreenNodeBuilder(ctx);
        builder.optional(TokenKind.TIMES, SyntaxKind.OPERATOR);
        GreenNode.Composite comp = (GreenNode.Composite) builder.node(SyntaxKind.IDENTIFIER_DEFINITION);
        assertEquals(1, comp.children().size());

        ParseContext ctx2 = context("x");
        GreenNodeBuilder builder2 = new GreenNodeBuilder(ctx2);
        builder2.optional(TokenKind.TIMES, SyntaxKind.OPERATOR);
        GreenNode.Composite comp2 = (GreenNode.Composite) builder2.node(SyntaxKind.IDENTIFIER_DEFINITION);
        assertTrue(comp2.children().isEmpty());
    }

    @Test
    void testExpected() {
        ParseContext ctx = context("x");
        GreenNodeBuilder builder = new GreenNodeBuilder(ctx);
        builder.expected(c -> c.consume(SyntaxKind.IDENTIFIER));
        builder.expected(SyntaxKind.EMPTY);
        builder.expected(GreenNode.Composite.create(SyntaxKind.SET, java.util.List.of()));

        GreenNode.Composite comp = (GreenNode.Composite) builder.node(SyntaxKind.STATEMENT_SEQUENCE);
        assertEquals(3, comp.children().size());
    }

    @Test
    void testExpectedTokenKindRule() {
        ParseContext ctx = context("123");
        GreenNodeBuilder builder = new GreenNodeBuilder(ctx);
        builder.expected(TokenKind.INT, c -> c.consume(SyntaxKind.INT_LITERAL), "Expected int");
        assertTrue(ctx.diagnostics().isEmpty());

        ParseContext ctxError = context("abc");
        GreenNodeBuilder builderError = new GreenNodeBuilder(ctxError);
        builderError.expected(TokenKind.INT, c -> c.consume(SyntaxKind.INT_LITERAL), "Expected int");
        assertEquals(1, ctxError.diagnostics().size());
        assertEquals("Expected int", ctxError.diagnostics().get(0).message());
    }

    @Test
    void testExpectedTokenKindSyntaxKind() {
        ParseContext ctx = context(";");
        GreenNodeBuilder builder = new GreenNodeBuilder(ctx);
        builder.expected(TokenKind.SEMICOLON, SyntaxKind.PUNCTUATION, "Expected ';'");
        assertTrue(ctx.diagnostics().isEmpty());

        ParseContext ctxError = context("123");
        GreenNodeBuilder builderError = new GreenNodeBuilder(ctxError);
        builderError.expected(TokenKind.SEMICOLON, SyntaxKind.PUNCTUATION, "Expected ';'");
        assertEquals(1, ctxError.diagnostics().size());
        assertEquals("Expected ';'", ctxError.diagnostics().get(0).message());
    }

    @Test
    void testKeywordAndPunctuation() {
        ParseContext ctx = context("MODULE ;");
        GreenNodeBuilder builder = new GreenNodeBuilder(ctx);
        builder.keyword(TokenKind.MODULE);
        builder.punctuation(TokenKind.SEMICOLON, ";");
        assertTrue(ctx.diagnostics().isEmpty());

        ParseContext ctxError = context("x");
        GreenNodeBuilder builderError = new GreenNodeBuilder(ctxError);
        builderError.keyword(TokenKind.MODULE);
        builderError.punctuation(TokenKind.SEMICOLON, ";");
        assertEquals(2, ctxError.diagnostics().size());
    }
}
