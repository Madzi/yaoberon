package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseForStatementTest extends AbstractDetailedParserSetup {

    @Test
    void testParseForStatement() {
        var context = context("FOR i := 1 TO 10 DO END");
        var node = parser.parseForStatement(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.FOR_STATEMENT, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
