package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseRecordTypeTest extends AbstractDetailedParserSetup {

    @Test
    void testParseRecordType() {
        var context = context("RECORD END");
        var node = parser.parseRecordType(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.RECORD_TYPE, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
