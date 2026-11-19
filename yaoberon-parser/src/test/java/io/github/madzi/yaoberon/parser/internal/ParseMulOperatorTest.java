package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseMulOperatorTest extends AbstractDetailedParserSetup {

    @Test
    void testParseMulOperator() {
        var context = context("*");
        var node = parser.parseMulOperator(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.OPERATOR, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
