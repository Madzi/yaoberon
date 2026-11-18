package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseCaseLabelListTest extends AbstractDetailedParserSetup {

    @Test
    void testParseCaseLabelList() {
        var context = context("1,2");
        var node = parser.parseCaseLabelList(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.CASE_LABEL_LIST, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
