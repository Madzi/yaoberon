package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseConstantDeclarationTest extends AbstractDetailedParserSetup {

    @Test
    void testParseConstantDeclaration() {
        var context = context("a = 1");
        var node = parser.parseConstantDeclaration(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.CONSTANT_DECLARATION, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
