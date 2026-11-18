package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseProcedureDeclarationTest extends AbstractDetailedParserSetup {

    @Test
    void testParseProcedureDeclaration() {
        var context = context("PROCEDURE foo; END foo");
        var node = parser.parseProcedureDeclaration(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.PROCEDURE_DECLARATION, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
