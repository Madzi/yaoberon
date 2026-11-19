package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseCaseTest extends AbstractDetailedParserSetup {

    @Test
    void testParseCase() {
        var context = context("1: a := 1");
        var node = parser.parseCase(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.CASE, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
