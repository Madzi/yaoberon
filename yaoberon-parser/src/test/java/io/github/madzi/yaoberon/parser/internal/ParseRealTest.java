package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseRealTest extends AbstractDetailedParserSetup {

    @Test
    void testParseValidReal() {
        var context = context("1.23");
        var node = parser.parseReal(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.REAL_LITERAL, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }

    @Test
    void testParseRealWithExponent() {
        var context = context("1.23E4");
        var node = parser.parseReal(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.REAL_LITERAL, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
