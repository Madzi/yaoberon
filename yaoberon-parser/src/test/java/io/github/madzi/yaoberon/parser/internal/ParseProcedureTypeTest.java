package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseProcedureTypeTest extends AbstractDetailedParserSetup {

    @Test
    void testParseProcedureType() {
        var context = context("PROCEDURE");
        var node = parser.parseProcedureType(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.PROCEDURE_TYPE, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
