package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseFormalParametersTest extends AbstractDetailedParserSetup {

    @Test
    void testParseFormalParameters() {
        var context = context("(a: INTEGER)");
        var node = parser.parseFormalParameters(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.FORMAL_PARAMETERS, node.kind());
        Assertions.assertTrue(context.errors().isEmpty());
    }
}
