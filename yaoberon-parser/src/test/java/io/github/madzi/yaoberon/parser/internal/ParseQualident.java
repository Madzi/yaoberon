package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ParseQualident extends AbstractDetailedParserSetup {

    @Test
    void testParseIdent() {
        var context = context("ident");
        var node = parser.parseQualident(context);
        Assertions.assertTrue(context.errors().isEmpty());
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.QUALIDENT, node.kind());
        Assertions.assertEquals("ident", fromNode(context.source(), node));
    }

    @Test
    void testParseQualident() {
        var context = context("prefix.ident");
        var node = parser.parseQualident(context);
        Assertions.assertTrue(context.errors().isEmpty());
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.QUALIDENT, node.kind());
        Assertions.assertEquals("prefix.ident", fromNode(context.source(), node));
    }

    @Test
    void testParseLongQualident() {
        var context = context("sub.prefix.ident");
        var node = parser.parseQualident(context);
        Assertions.assertTrue(context.errors().isEmpty());
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.QUALIDENT, node.kind());
        Assertions.assertEquals("sub.prefix", fromNode(context.source(), node));
    }
}
