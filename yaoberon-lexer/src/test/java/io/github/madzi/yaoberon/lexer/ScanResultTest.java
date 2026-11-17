package io.github.madzi.yaoberon.lexer;

import io.github.madzi.yaoberon.lexer.token.Token;
import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.source.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ScanResultTest {

    @Test
    void testScanResultProperties() {
        Token t = new Token(TokenKind.EOT, new Span(0, 0), List.of());
        ScanResult result = new ScanResult(List.of(t), LexerState.INITIAL, 0);

        assertEquals(List.of(t), result.tokens());
        assertEquals(LexerState.INITIAL, result.finalState());
        assertEquals(0, result.stoppedAtOffset());

        ScanResult same = new ScanResult(List.of(t), LexerState.INITIAL, 0);
        assertEquals(same, result);
        assertEquals(same.hashCode(), result.hashCode());
        assertNotNull(result.toString());
    }

    @Test
    void testDefensiveCopy() {
        Token t = new Token(TokenKind.EOT, new Span(0, 0), List.of());
        java.util.ArrayList<Token> mutable = new java.util.ArrayList<>();
        mutable.add(t);

        ScanResult result = new ScanResult(mutable, LexerState.INITIAL, 0);
        mutable.clear();
        assertEquals(1, result.tokens().size());

        assertThrows(UnsupportedOperationException.class, () -> result.tokens().add(t));

        ScanResult nullTokens = new ScanResult(null, LexerState.INITIAL, 0);
        assertTrue(nullTokens.tokens().isEmpty());
    }
}
