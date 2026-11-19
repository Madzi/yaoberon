package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseModuleTest extends AbstractDetailedParserSetup {

    @Test
    void testParseModule() {
        var context = context("MODULE M; END M.");
        var node = parser.parseModule(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.MODULE_DECLARATION, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
