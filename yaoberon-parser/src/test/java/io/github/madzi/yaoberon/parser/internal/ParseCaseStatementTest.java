package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseCaseStatementTest extends AbstractDetailedParserSetup {

    @Test
    void testParseCaseStatement() {
        var context = context("CASE x OF 1: END");
        var node = parser.parseCaseStatement(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.CASE_STATEMENT, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
