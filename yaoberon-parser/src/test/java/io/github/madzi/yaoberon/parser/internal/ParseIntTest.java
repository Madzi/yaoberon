package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseIntTest extends AbstractDetailedParserSetup {

    @Test
    void testParseValidInt() {
        var context = context("123");
        var node = parser.parseInt(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.INT_LITERAL, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }

    @Test
    void testParseHexInt() {
        var context = context("1AH");
        var node = parser.parseInt(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.INT_LITERAL, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
