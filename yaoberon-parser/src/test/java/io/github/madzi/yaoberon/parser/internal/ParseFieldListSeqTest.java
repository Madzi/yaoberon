package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseFieldListSeqTest extends AbstractDetailedParserSetup {

    @Test
    void testParseFieldListSeq() {
        var context = context("a: INTEGER");
        var node = parser.parseFieldListSeq(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.FIELD_LIST_SEQUENCE, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
