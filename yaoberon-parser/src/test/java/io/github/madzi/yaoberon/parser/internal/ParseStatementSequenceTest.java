package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseStatementSequenceTest extends AbstractDetailedParserSetup {

    @Test
    void testParseStatementSequence() {
        var context = context("a := 1; b := 2");
        var node = parser.parseStatementSequence(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.STATEMENT_SEQUENCE, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
