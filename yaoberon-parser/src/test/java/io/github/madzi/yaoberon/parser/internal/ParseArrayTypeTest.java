package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseArrayTypeTest extends AbstractDetailedParserSetup {

    @Test
    void testParseArrayType() {
        var context = context("ARRAY 10 OF INTEGER");
        var node = parser.parseArrayType(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.ARRAY_TYPE, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
