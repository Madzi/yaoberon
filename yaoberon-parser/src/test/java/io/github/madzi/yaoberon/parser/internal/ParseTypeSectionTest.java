package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseTypeSectionTest extends AbstractDetailedParserSetup {

    @Test
    void testParseTypeSection() {
        var context = context("TYPE a = INTEGER;");
        var node = parser.parseTypeSection(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.TYPE_SECTION, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
