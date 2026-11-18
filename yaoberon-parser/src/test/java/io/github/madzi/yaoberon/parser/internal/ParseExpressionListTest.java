package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseExpressionListTest extends AbstractDetailedParserSetup {

    @Test
    void testParseExpressionList() {
        var context = context("1,2");
        var node = parser.parseExpressionList(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.EXPRESSION_LIST, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
