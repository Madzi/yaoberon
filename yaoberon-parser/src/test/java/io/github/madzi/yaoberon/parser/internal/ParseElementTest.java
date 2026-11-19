package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseElementTest extends AbstractDetailedParserSetup {

    @Test
    void testParseElement() {
        var context = context("1..10");
        var node = parser.parseElement(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.ELEMENT, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
