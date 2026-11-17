package io.github.madzi.yaoberon.parser;

import io.github.madzi.yaoberon.lexer.Lexer;
import io.github.madzi.yaoberon.lexer.ScanResult;
import io.github.madzi.yaoberon.lexer.token.Token;
import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import io.github.madzi.yaoberon.source.SourceFile;
import io.github.madzi.yaoberon.source.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParseContextTest {

    private final Lexer lexer = Lexer.create();

    private ParseContext context(String text) {
        SourceFile source = SourceFile.create("test://code", 1, text);
        ScanResult scan = lexer.tokenizeAll(source);
        return new ParseContext(source, scan.tokens());
    }

    @Test
    void testBasicAccessors() {
        ParseContext ctx = context("A B");
        assertEquals(3, ctx.tokens().size()); // A, B, EOT
        assertTrue(ctx.errors().isEmpty());
        assertEquals(0, ctx.cursor().get());
        assertEquals("test://code", ctx.source().uri());

        assertEquals(TokenKind.IDENT, ctx.peek().kind());
        assertEquals("A", ctx.getSourceText());

        Token first = ctx.next();
        assertEquals(TokenKind.IDENT, first.kind());
        assertEquals(1, ctx.cursor().get());
        assertEquals(TokenKind.IDENT, ctx.peek().kind());
        assertEquals("B", ctx.getSourceText());
    }

    @Test
    void testCheckAndInList() {
        ParseContext ctx = context("MODULE Test;");
        assertTrue(ctx.check(TokenKind.MODULE));
        assertFalse(ctx.check(TokenKind.BEGIN));
        assertTrue(ctx.inList(TokenKind.MODULE, TokenKind.BEGIN));
        assertFalse(ctx.inList(TokenKind.PROCEDURE, TokenKind.END));

        // Consume until end
        while (!ctx.isAtEnd()) {
            ctx.next();
        }
        assertTrue(ctx.isAtEnd());
        assertFalse(ctx.check(TokenKind.MODULE));
        assertFalse(ctx.inList(TokenKind.MODULE));
    }

    @Test
    void testConsume() {
        ParseContext ctx = context("x");
        GreenNode node = ctx.consume(SyntaxKind.IDENTIFIER);
        assertTrue(node.isLeaf());
        assertEquals(SyntaxKind.IDENTIFIER, node.kind());
        assertTrue(ctx.isAtEnd());
    }

    @Test
    void testReportError() {
        ParseContext ctx = context("A B");
        ctx.reportError("Error at A");
        assertEquals(1, ctx.errors().size());
        assertEquals(new Span(0, 1), ctx.errors().get(0).span());
        assertEquals("Error at A", ctx.errors().get(0).message());

        // Advance to end
        ctx.next(); // A
        ctx.next(); // B
        ctx.next(); // EOT
        assertTrue(ctx.isAtEnd());

        ctx.reportError("Error at EOF");
        assertEquals(2, ctx.errors().size());
        assertEquals("Error at EOF", ctx.errors().get(1).message());
    }

    @Test
    void testIsNextTokenIdentifier() {
        ParseContext ctx = context("A B 123");
        // cursor at A, next is B (IDENT)
        assertTrue(ctx.isNextTokenIdentifier());
        ctx.next(); // move to B
        // cursor at B, next is 123 (INT)
        assertFalse(ctx.isNextTokenIdentifier());
        ctx.next(); // move to 123
        // next is EOT
        assertFalse(ctx.isNextTokenIdentifier());
        ctx.next(); // move to EOT
        assertFalse(ctx.isNextTokenIdentifier());
    }

    @Test
    void testIsTypeGuard() {
        // Simple type guard: ( T ) .
        ParseContext ctx1 = context("( T ) .");
        assertTrue(ctx1.isTypeGuard());

        // Type guard with bracket: ( T ) [
        ParseContext ctx2 = context("( T ) [");
        assertTrue(ctx2.isTypeGuard());

        // Type guard with arrow: ( T ) ^
        ParseContext ctx3 = context("( T ) ^");
        assertTrue(ctx3.isTypeGuard());

        // Type guard with qualident: ( M . T ) .
        ParseContext ctx4 = context("( M . T ) .");
        assertTrue(ctx4.isTypeGuard());

        // Not a type guard: ( 123 ) .
        ParseContext ctx5 = context("( 123 ) .");
        assertFalse(ctx5.isTypeGuard());

        // Not a type guard (not followed by selector): ( T ) +
        ParseContext ctx6 = context("( T ) +");
        assertFalse(ctx6.isTypeGuard());

        // Incomplete expression
        ParseContext ctx7 = context("(");
        assertFalse(ctx7.isTypeGuard());
    }
}
