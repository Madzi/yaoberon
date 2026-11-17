package io.github.madzi.yaoberon.lexer.token;

import io.github.madzi.yaoberon.source.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TokenTest {

    @Test
    void testTokenCreationWithNullTrivias() {
        Token token = new Token(TokenKind.MODULE, new Span(0, 6), null);
        assertNotNull(token.trivias());
        assertTrue(token.trivias().isEmpty());
        assertEquals(TokenKind.MODULE, token.kind());
        assertEquals(new Span(0, 6), token.span());
        assertTrue(token.is(TokenKind.MODULE));
        assertFalse(token.is(TokenKind.IDENT));
        assertFalse(token.isEot());
    }

    @Test
    void testTokenCreationWithTrivias() {
        Trivia ws = new Trivia.Whitespace(new Span(0, 2));
        Trivia comment = new Trivia.Comment(new Span(2, 8), true);
        Token token = new Token(TokenKind.IDENT, new Span(10, 3), List.of(ws, comment));

        assertEquals(2, token.trivias().size());
        assertEquals(ws, token.trivias().get(0));
        assertEquals(comment, token.trivias().get(1));
        assertFalse(token.isEot());
        assertTrue(token.is(TokenKind.IDENT));
    }

    @Test
    void testTokenIsEot() {
        Token token = new Token(TokenKind.EOT, new Span(5, 0), List.of());
        assertTrue(token.isEot());
        assertTrue(token.is(TokenKind.EOT));
    }

    @Test
    void testTokenEqualsAndHashCode() {
        Token t1 = new Token(TokenKind.PLUS, new Span(0, 1), List.of());
        Token t2 = new Token(TokenKind.PLUS, new Span(0, 1), List.of());
        Token t3 = new Token(TokenKind.MINUS, new Span(0, 1), List.of());

        assertEquals(t1, t2);
        assertEquals(t1.hashCode(), t2.hashCode());
        assertNotEquals(t1, t3);
        assertNotNull(t1.toString());
    }

    @Test
    void testTriviaWhitespace() {
        Trivia.Whitespace ws = new Trivia.Whitespace(new Span(0, 4));
        assertEquals(new Span(0, 4), ws.span());
        assertEquals(new Trivia.Whitespace(new Span(0, 4)), ws);
        assertEquals(new Trivia.Whitespace(new Span(0, 4)).hashCode(), ws.hashCode());
        assertNotNull(ws.toString());
    }

    @Test
    void testTriviaComment() {
        Trivia.Comment c1 = new Trivia.Comment(new Span(0, 10), true);
        assertEquals(new Span(0, 10), c1.span());
        assertTrue(c1.isNestedCorrectly());

        Trivia.Comment c2 = new Trivia.Comment(new Span(0, 10), false);
        assertFalse(c2.isNestedCorrectly());
        assertNotEquals(c1, c2);
        assertNotNull(c1.toString());
    }

    @Test
    void testTokenKindEnum() {
        for (TokenKind kind : TokenKind.values()) {
            assertEquals(kind, TokenKind.valueOf(kind.name()));
        }
    }
}
