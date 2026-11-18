package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseProcedureCallTest extends AbstractDetailedParserSetup {

    @Test
    void testParseProcedureCall() {
        var context = context("foo(1)");
        var node = parser.parseProcedureCall(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.PROCEDURE_CALL_STATEMENT, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
