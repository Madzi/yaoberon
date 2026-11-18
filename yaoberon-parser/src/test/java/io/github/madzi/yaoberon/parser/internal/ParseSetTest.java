package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseSetTest extends AbstractDetailedParserSetup {

    @Test
    void testParseSet() {
        var context = context("{1,2}");
        var node = parser.parseSet(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.SET, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
