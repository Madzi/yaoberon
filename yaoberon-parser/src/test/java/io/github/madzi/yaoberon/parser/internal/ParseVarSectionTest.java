package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseVarSectionTest extends AbstractDetailedParserSetup {

    @Test
    void testParseVarSection() {
        var context = context("VAR a: INTEGER;");
        var node = parser.parseVarSection(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.VAR_SECTION, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
