package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseTypeDeclarationTest extends AbstractDetailedParserSetup {

    @Test
    void testParseTypeDeclaration() {
        var context = context("myType = INTEGER");
        var node = parser.parseTypeDeclaration(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.TYPE_DECLARATION, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
