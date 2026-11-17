package io.github.madzi.yaoberon.source;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SpanTest {

    @Test
    void testSpanProperties() {
        Span span = new Span(5, 10);
        assertEquals(5, span.start());
        assertEquals(10, span.length());
        assertEquals(15, span.end());
    }

    @Test
    void testSpanEnd() {
        Span span = new Span(0, 0);
        assertEquals(0, span.end());

        Span span1 = new Span(10, 5);
        assertEquals(15, span1.end());

        Span span2 = new Span(-5, 5);
        assertEquals(0, span2.end());
    }
}
