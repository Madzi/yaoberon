package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseWhileStatementTest extends AbstractDetailedParserSetup {

    @Test
    void testParseWhileStatement() {
        var context = context("WHILE TRUE DO END");
        var node = parser.parseWhileStatement(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.WHILE_STATEMENT, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
