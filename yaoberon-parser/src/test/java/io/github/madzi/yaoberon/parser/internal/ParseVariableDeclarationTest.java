package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseVariableDeclarationTest extends AbstractDetailedParserSetup {

    @Test
    void testParseVariableDeclaration() {
        var context = context("a: INTEGER");
        var node = parser.parseVariableDeclartion(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.VARIABLE_DECLARATION, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
