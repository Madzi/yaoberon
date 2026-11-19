package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ParsePointerTypeTest extends AbstractDetailedParserSetup {

    @Test
    void testParsePointerType() {
        var context = context("POINTER TO INTEGER");
        var node = parser.parsePointerType(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.POINTER_TYPE, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
    }
}
