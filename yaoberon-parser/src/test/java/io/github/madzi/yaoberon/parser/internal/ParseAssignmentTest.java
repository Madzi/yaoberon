package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseAssignmentTest extends AbstractDetailedParserSetup {

    @Test
    void testParseAssignment() {
        var context = context("a := 1");
        var node = parser.parseAssignment(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.ASSIGNMENT_STATEMENT, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
