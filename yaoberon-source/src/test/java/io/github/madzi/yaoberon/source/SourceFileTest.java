package io.github.madzi.yaoberon.source;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SourceFileTest {

    @Test
    void testCreateAndGetters() {
        String uri = "file:///test.oberon";
        int version = 1;
        String content = "print(1)";
        SourceFile sourceFile = SourceFile.create(uri, version, content);

        assertEquals(uri, sourceFile.uri());
        assertEquals(version, sourceFile.version());
        assertEquals(content, sourceFile.text());
        assertEquals(content.length(), sourceFile.length());
    }

    @Test
    void testPiece() {
        String uri = "file:///test.oberon";
        int version = 1;
        String content = "print(1)";
        SourceFile sourceFile = SourceFile.create(uri, version, content);

        // Test full range
        Span fullSpan = new Span(0, content.length());
        assertEquals(content, sourceFile.piece(fullSpan));

        // Test partial range
        Span partialSpan = new Span(0, 5); // "print"
        assertEquals("print", sourceFile.piece(partialSpan));

        // Test single character
        Span singleSpan = new Span(5, 1); // "("
        assertEquals("(", sourceFile.piece(singleSpan));
    }
}
