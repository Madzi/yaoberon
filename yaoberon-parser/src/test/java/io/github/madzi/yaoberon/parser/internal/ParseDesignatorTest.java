package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseDesignatorTest extends AbstractDetailedParserSetup {

    @Test
    void testParseDesignator() {
        var context = context("a.b");
        var node = parser.parseDesignator(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.DESIGNATOR, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
