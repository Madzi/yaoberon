package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseIdentListTest extends AbstractDetailedParserSetup {

    @Test
    void testParseIdentList() {
        var context = context("a, b");
        var node = parser.parseIdentList(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.IDENTIFIER_LIST, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
