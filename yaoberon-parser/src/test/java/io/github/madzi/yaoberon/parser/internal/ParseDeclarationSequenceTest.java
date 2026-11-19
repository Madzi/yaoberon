package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParseDeclarationSequenceTest extends AbstractDetailedParserSetup {

    @Test
    void testParseDeclarationSequence() {
        var context = context("");
        var node = parser.parseDeclarationSequence(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.DECLARATION_SEQUENCE, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
