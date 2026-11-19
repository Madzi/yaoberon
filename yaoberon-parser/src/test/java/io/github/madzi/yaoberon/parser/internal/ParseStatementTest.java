package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseStatementTest extends AbstractDetailedParserSetup {

    @Test
    void testParseAssignmentStatement() {
        var context = context("a := 1");
        var node = parser.parseStatement(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.ASSIGNMENT_STATEMENT, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
