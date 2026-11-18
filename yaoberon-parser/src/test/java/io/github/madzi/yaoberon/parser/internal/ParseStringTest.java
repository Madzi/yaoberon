package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseStringTest extends AbstractDetailedParserSetup {

    @Test
    void testParseStringLiteral() {
        var context = context("'hello'");
        var node = parser.parseString(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.STRING_LITERAL, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }

    @Test
    void testParseCharLiteral() {
        var context = context("'a'");
        var node = parser.parseString(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.STRING_LITERAL, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
