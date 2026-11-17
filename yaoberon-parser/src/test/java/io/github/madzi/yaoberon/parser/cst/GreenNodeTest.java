package io.github.madzi.yaoberon.parser.cst;

import io.github.madzi.yaoberon.lexer.token.Token;
import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.lexer.token.Trivia;
import io.github.madzi.yaoberon.source.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GreenNodeTest {

    @Test
    void testTokenLeaf() {
        Trivia ws = new Trivia.Whitespace(new Span(0, 2));
        Token token = new Token(TokenKind.IDENT, new Span(2, 5), List.of(ws));
        GreenNode.TokenLeaf leaf = new GreenNode.TokenLeaf(SyntaxKind.IDENTIFIER, token);

        assertEquals(SyntaxKind.IDENTIFIER, leaf.kind());
        assertEquals(token, leaf.token());
        assertTrue(leaf.isLeaf());
        // textLength = trivias length (2) + token span length (5) = 7
        assertEquals(7, leaf.textLength());

        GreenNode.TokenLeaf same = new GreenNode.TokenLeaf(SyntaxKind.IDENTIFIER, token);
        assertEquals(same, leaf);
        assertEquals(same.hashCode(), leaf.hashCode());
        assertNotNull(leaf.toString());
    }

    @Test
    void testComposite() {
        Token t1 = new Token(TokenKind.MODULE, new Span(0, 6), List.of());
        Token t2 = new Token(TokenKind.IDENT, new Span(7, 4), List.of(new Trivia.Whitespace(new Span(6, 1))));

        GreenNode.TokenLeaf l1 = new GreenNode.TokenLeaf(SyntaxKind.KEYWORD, t1);
        GreenNode.TokenLeaf l2 = new GreenNode.TokenLeaf(SyntaxKind.IDENTIFIER, t2);

        GreenNode.Composite composite = GreenNode.Composite.create(SyntaxKind.MODULE_DECLARATION, List.of(l1, l2));

        assertEquals(SyntaxKind.MODULE_DECLARATION, composite.kind());
        assertFalse(composite.isLeaf());
        // l1 length = 6, l2 length = 1 + 4 = 5. Total = 11
        assertEquals(11, composite.textLength());
        assertEquals(2, composite.children().size());

        GreenNode.Composite same = GreenNode.Composite.create(SyntaxKind.MODULE_DECLARATION, List.of(l1, l2));
        assertEquals(same, composite);
        assertEquals(same.hashCode(), composite.hashCode());
        assertNotNull(composite.toString());
    }

    @Test
    void testSyntaxKindEnum() {
        for (SyntaxKind kind : SyntaxKind.values()) {
            assertEquals(kind, SyntaxKind.valueOf(kind.name()));
        }
    }
}
