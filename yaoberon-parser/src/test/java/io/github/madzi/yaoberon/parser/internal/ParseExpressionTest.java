package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseExpressionTest extends AbstractDetailedParserSetup {

    @Test
    void testParseExpression() {
        var context = context("1 + 2");
        var node = parser.parseExpression(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.EXPRESSION, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
