package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.cst.GreenNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import java.util.List;

class TrickyPlacesTest extends AbstractDetailedParserSetup {

    @Test
    void testComplexDesignator() {
        var source = "a.a.a";
        var context = context(source);
        var node = parser.parseDesignator(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.DESIGNATOR, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
        assertChildKinds(node, SyntaxKind.IDENTIFIER, SyntaxKind.SELECTOR, SyntaxKind.SELECTOR);
        Assertions.assertTrue(context.isAtEnd());
        Assertions.assertEquals(source.length(), node.textLength());
    }

    @Test
    void testComplexParse() {
        var source = "rec.field[idx]^.next";
        var context = context(source);
        var node = parser.parseDesignator(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.DESIGNATOR, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
        assertChildKinds(node, SyntaxKind.IDENTIFIER, SyntaxKind.SELECTOR, SyntaxKind.SELECTOR, SyntaxKind.SELECTOR, SyntaxKind.SELECTOR);
        Assertions.assertTrue(context.isAtEnd());
        Assertions.assertEquals(source.length(), node.textLength());
    }

    @Test
    void testDesignator() {
        var source = "a.b";
        var context = context(source);
        var node = parser.parseDesignator(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.DESIGNATOR, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
        assertChildKinds(node, SyntaxKind.IDENTIFIER, SyntaxKind.SELECTOR);
        Assertions.assertTrue(context.isAtEnd());
        Assertions.assertEquals(source.length(), node.textLength());
    }

    @Test
    void testVariableDeclaration() {
        var source = "x: M.T";
        var context = context(source);
        var node = parser.parseVariableDeclaration(context);
        Assertions.assertNotNull(node);
        Assertions.assertEquals(SyntaxKind.VARIABLE_DECLARATION, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
        assertChildKinds(node, SyntaxKind.IDENTIFIER_LIST, SyntaxKind.PUNCTUATION, SyntaxKind.QUALIDENT);
        Assertions.assertTrue(context.isAtEnd());
        Assertions.assertEquals(source.length(), node.textLength());
    }

    @Test
    void testAssignmentWithDesignators() {
        var source = "a.b := c.d";
        var context = context(source);
        var node = parser.parseStatement(context);
        Assertions.assertEquals(SyntaxKind.ASSIGNMENT_STATEMENT, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
        var composite = Assertions.assertInstanceOf(GreenNode.Composite.class, node);
        Assertions.assertEquals(SyntaxKind.DESIGNATOR, composite.children().get(0).kind());
        Assertions.assertTrue(context.isAtEnd());
        Assertions.assertEquals(source.length(), node.textLength());
    }

    @Test
    void testProcedureCallWithModule() {
        var source = "M.Proc(x)";
        var context = context(source);
        var node = parser.parseStatement(context);
        Assertions.assertEquals(SyntaxKind.PROCEDURE_CALL_STATEMENT, node.kind());
        Assertions.assertTrue(context.diagnostics().isEmpty());
        Assertions.assertTrue(context.isAtEnd());
    }

    @Test
    void testExpressionWithDesignator() {
        var source = "a.a.a + 1";
        var context = context(source);
        var node = parser.parseExpression(context);
        Assertions.assertTrue(context.diagnostics().isEmpty());
        Assertions.assertTrue(context.isAtEnd());
        Assertions.assertEquals(source.length(), node.textLength());
    }

    private static void assertChildKinds(GreenNode node, SyntaxKind... expected) {
        var composite = Assertions.assertInstanceOf(GreenNode.Composite.class, node);
        var actual = composite.children().stream().map(GreenNode::kind).toList();
        Assertions.assertEquals(List.of(expected), actual);
    }
}
