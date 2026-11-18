package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseNumberTest extends AbstractDetailedParserSetup {

    @Test
    void testParseIntNumber() {
        var context = context("123");
        var node = parser.parseNumber(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.INT_LITERAL, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }

    @Test
    void testParseRealNumber() {
        var context = context("1.23");
        var node = parser.parseNumber(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.REAL_LITERAL, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }

    @Test
    void testParseInvalidNumber() {
        var context = context("abc");
        var node = parser.parseNumber(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.ERROR, node.kind());
        Assertions.assertFalse(context.errors().isEmpty());
    }
}
