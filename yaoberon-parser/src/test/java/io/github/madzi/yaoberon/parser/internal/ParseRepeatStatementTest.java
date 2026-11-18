package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseRepeatStatementTest extends AbstractDetailedParserSetup {

    @Test
    void testParseRepeatStatement() {
        var context = context("REPEAT UNTIL TRUE");
        var node = parser.parseRepeatStatement(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.REPEAT_STATEMENT, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
