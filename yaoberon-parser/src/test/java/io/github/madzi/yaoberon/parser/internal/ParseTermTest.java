package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseTermTest extends AbstractDetailedParserSetup {

    @Test
    void testParseTerm() {
        var context = context("1 * 2");
        var node = parser.parseTerm(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.TERM, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
