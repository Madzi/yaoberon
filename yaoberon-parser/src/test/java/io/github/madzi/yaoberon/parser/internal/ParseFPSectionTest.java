package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseFPSectionTest extends AbstractDetailedParserSetup {

    @Test
    void testParseFPSection() {
        var context = context("a: INTEGER");
        var node = parser.parseFPSection(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.FP_SECTION, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
