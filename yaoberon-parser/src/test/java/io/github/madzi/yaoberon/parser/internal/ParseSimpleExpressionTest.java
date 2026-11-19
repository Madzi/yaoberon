package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseSimpleExpressionTest extends AbstractDetailedParserSetup {

    @Test
    void testParseSimpleExpression() {
        var context = context("1 + 2");
        var node = parser.parseSimpleExpression(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.SIMPLE_EXPRESSION, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
