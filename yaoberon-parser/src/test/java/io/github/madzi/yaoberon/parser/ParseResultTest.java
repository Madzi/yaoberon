package io.github.madzi.yaoberon.parser;

import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import io.github.madzi.yaoberon.source.Span;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ParseResultTest {

    @Test
    void testParseResultProperties() {
        GreenNode root = GreenNode.Composite.create(SyntaxKind.MODULE_DECLARATION, List.of());
        SyntaxError error = new SyntaxError(new Span(0, 1), "Error");
        ParseResult result = new ParseResult(root, List.of(error));

        assertEquals(root, result.root());
        assertEquals(List.of(error), result.syntaxErrors());

        ParseResult same = new ParseResult(root, List.of(error));
        assertEquals(same, result);
        assertEquals(same.hashCode(), result.hashCode());
        assertNotNull(result.toString());
    }
}
