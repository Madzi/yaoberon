package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseImportTest extends AbstractDetailedParserSetup {

    @Test
    void testParseImport() {
        var context = context("A");
        var node = parser.parseImport(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.IMPORT_DECLARATION, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
