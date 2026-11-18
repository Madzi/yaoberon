package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseAddOpTest extends AbstractDetailedParserSetup {

    @Test
    void testParseAddOp() {
        var context = context("+");
        var node = parser.parseAddOp(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.OPERATOR, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
