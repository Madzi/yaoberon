package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseLabelRangeTest extends AbstractDetailedParserSetup {

    @Test
    void testParseLabelRange() {
        var context = context("1..10");
        var node = parser.parseLabelRange(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.LABEL_RANGE, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
