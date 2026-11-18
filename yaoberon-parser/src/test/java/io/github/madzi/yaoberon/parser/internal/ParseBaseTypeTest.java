package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseBaseTypeTest extends AbstractDetailedParserSetup {

    @Test
    void testParseBaseType() {
        var context = context("INTEGER");
        var node = parser.parseBaseType(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.QUALIDENT, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
