package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseIdent extends AbstractDetailedParserSetup {
    
    @Test
    void testParseCorrectIdent() {
        var context = context("ident");
        var node = parser.parseIdent(context);
        Assertions.assertTrue(context.errors().isEmpty());
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.IDENTIFIER, node.kind());
    }

    @Test
    void testParseIncorrectIdent() {
        var context = context("$ident");
        var node = parser.parseIdent(context);
        Assertions.assertFalse(context.errors().isEmpty());
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.ERROR, node.kind());
    }
}
