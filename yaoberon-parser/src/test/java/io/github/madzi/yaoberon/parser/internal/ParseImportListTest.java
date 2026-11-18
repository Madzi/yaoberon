package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseImportListTest extends AbstractDetailedParserSetup {

    @Test
    void testParseImportList() {
        var context = context("IMPORT A;");
        var node = parser.parseImportList(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.IMPORT_SECTION, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
