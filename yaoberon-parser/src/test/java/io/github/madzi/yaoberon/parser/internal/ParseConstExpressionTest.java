package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseConstExpressionTest extends AbstractDetailedParserSetup {

    @Test
    void testParseConstExpression() {
        var context = context("1 + 2");
        var node = parser.parseConstExpression(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.EXPRESSION, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
