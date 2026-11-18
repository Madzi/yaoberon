package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseSelectorTest extends AbstractDetailedParserSetup {

    @Test
    void testParseSelector() {
        var context = context(".a");
        var node = parser.parseSelector(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.SELECTOR, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
