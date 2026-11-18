package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseProcedureBodyTest extends AbstractDetailedParserSetup {

    @Test
    void testParseProcedureBody() {
        var context = context("END");
        var node = parser.parseProcedureBody(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.PROCEDURE_BODY, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
