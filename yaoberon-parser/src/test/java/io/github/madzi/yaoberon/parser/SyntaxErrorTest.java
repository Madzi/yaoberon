package io.github.madzi.yaoberon.parser;

import io.github.madzi.yaoberon.source.Span;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SyntaxErrorTest {

    @Test
    void testSyntaxErrorProperties() {
        Span span = new Span(5, 10);
        String message = "Unexpected token";
        SyntaxError error = new SyntaxError(span, message);

        assertEquals(span, error.span());
        assertEquals(message, error.message());

        SyntaxError same = new SyntaxError(span, message);
        assertEquals(same, error);
        assertEquals(same.hashCode(), error.hashCode());
        assertNotNull(error.toString());
    }
}
