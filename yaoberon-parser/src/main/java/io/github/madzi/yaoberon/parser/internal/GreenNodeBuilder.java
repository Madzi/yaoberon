package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.parser.ParseContext;
import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import java.util.ArrayList;
import java.util.List;

public class GreenNodeBuilder {

    private final ParseContext context;
    private final List<GreenNode> children;

    public GreenNodeBuilder(final ParseContext context) {
        this.context = context;
        this.children = new ArrayList<>();
    }

    public void separatedSequence(final TokenKind itemCheckToken, final TokenKind separator, final SyntaxKind separatorSyntax, final ParseRule itemRule) {
        if (!context.isAtEnd() && context.check(itemCheckToken)) {
            children.add(itemRule.apply(context));
            while (context.check(separator)) {
                children.add(context.consume(separatorSyntax));
                if (!context.check(itemCheckToken)) {
                    break;
                }
                children.add(itemRule.apply(context));
            }
        }
    }

    public void sequence(final TokenKind kind, final ParseRule rule) {
        while (!context.isAtEnd() && context.check(kind)) {
            children.add(rule.apply(context));
        }
    }

    public void optional(final TokenKind kind, final ParseRule rule) {
        if (context.check(kind)) {
            children.add(rule.apply(context));
        }
    }

    public void optional(final TokenKind kind, final SyntaxKind syntaxKind) {
        if (context.check(kind)) {
            children.add(context.consume(syntaxKind));
        }
    }

    public void expected(final ParseRule rule) {
        children.add(rule.apply(context));
    }

    public void expected(final SyntaxKind kind) {
        children.add(context.consume(kind));
    }

    public void expected(final GreenNode node) {
        children.add(node);
    }

    public void expected(final TokenKind tokenKind, final ParseRule rule, final String message) {
        if (context.check(tokenKind)) {
            children.add(rule.apply(context));
        } else {
            context.reportError(message);
        }
    }

    public void expected(final TokenKind tokenKind, final SyntaxKind syntaxKind, final String message) {
        if (context.check(tokenKind)) {
            children.add(context.consume(syntaxKind));
        } else {
            context.reportError(message);
        }
    }

    public void keyword(final TokenKind tokenKind) {
        expected(tokenKind, SyntaxKind.KEYWORD, "Expected '" + tokenKind.name() + "'");
    }

    public void punctuation(final TokenKind tokenKind, final String message) {
        expected(tokenKind, SyntaxKind.PUNCTUATION, "Expected '" + message + "'");
    }

    public GreenNode node(final SyntaxKind kind) {
        return GreenNode.Composite.create(kind, children);
    }
}
