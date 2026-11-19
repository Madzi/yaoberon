package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseConstSectionTest extends AbstractDetailedParserSetup {

    @Test
    void testParseConstSection() {
        var context = context("CONST a = 1;");
        var node = parser.parseConstSection(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.CONST_SECTION, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
