package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseIfStatementTest extends AbstractDetailedParserSetup {

    @Test
    void testParseIfStatement() {
        var context = context("IF TRUE THEN END");
        var node = parser.parseIfStatement(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.IF_STATEMENT, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
