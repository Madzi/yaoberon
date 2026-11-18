package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseActualParametersTest extends AbstractDetailedParserSetup {

    @Test
    void testParseActualParameters() {
        var context = context("(1,2)");
        var node = parser.parseActualParameters(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.ACTUAL_PARAMETERS, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
