package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseFactorTest extends AbstractDetailedParserSetup {

    @Test
    void testParseFactor() {
        var context = context("1");
        var node = parser.parseFactor(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.INT_LITERAL, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
