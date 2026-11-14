package io.github.madzi.yaoberon.parser.cst;

import io.github.madzi.yaoberon.lexer.token.Token;
import java.util.List;

public interface GreenNode {

    SyntaxKind kind();

    int textLength();

    default boolean isLeaf() {
        return this instanceof TokenLeaf;
    }

    record Composite(SyntaxKind kind, int textLength, List<GreenNode> children) implements GreenNode {
        public Composite {
            children = List.copyOf(children);
        }
        public static Composite create(final SyntaxKind kind, final List<GreenNode> children) {
            int length = 0;
            for (GreenNode child : children) {
                length += child.textLength();
            }
            return new Composite(kind, length, children);
        }
    }

    record TokenLeaf(SyntaxKind kind, Token token) implements GreenNode {
        @Override
        public int textLength() {
            int triviasLen = token.trivias().stream().mapToInt(t -> t.span().length()).sum();
            return triviasLen + token.span().length();
        }
    }
}
