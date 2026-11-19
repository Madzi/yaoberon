package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseProcedureHeadingTest extends AbstractDetailedParserSetup {

    @Test
    void testParseProcedureHeading() {
        var context = context("PROCEDURE foo");
        var node = parser.parseProcedureHeading(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.PROCEDURE_HEADING, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
