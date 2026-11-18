package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseLengthTest extends AbstractDetailedParserSetup {

    @Test
    void testParseLength() {
        var context = context("10");
        var node = parser.parseLength(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.EXPRESSION, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
