package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParseIdentDefTest extends AbstractDetailedParserSetup {

    @Test
    void testParseIdentDef() {
        var context = context("ident");
        var node = parser.parseIdentDef(context);
        Assertions.assertTrue(context.errors().isEmpty());
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.IDENTIFIER_DEFINITION, node.kind());
        Assertions.assertEquals("ident", fromNode(context.source(), node));
    }

    @Test
    void testParseIdentDefExported() {
        var context = context("ident*");
        var node = parser.parseIdentDef(context);
        Assertions.assertTrue(context.errors().isEmpty());
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.IDENTIFIER_DEFINITION, node.kind());
        Assertions.assertEquals("ident*", fromNode(context.source(), node));
    }
}
