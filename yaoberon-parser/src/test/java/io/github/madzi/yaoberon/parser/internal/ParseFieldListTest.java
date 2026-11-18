package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseFieldListTest extends AbstractDetailedParserSetup {

    @Test
    void testParseFieldList() {
        var context = context("a: INTEGER");
        var node = parser.parseFieldList(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.FIELD_LIST, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
