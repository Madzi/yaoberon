package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseFormalTypeTest extends AbstractDetailedParserSetup {

    @Test
    void testParseFormalType() {
        var context = context("INTEGER");
        var node = parser.parseFormalType(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.FORMAL_TYPE, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
