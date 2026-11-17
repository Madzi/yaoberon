package io.github.madzi.yaoberon.symbol;

import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import io.github.madzi.yaoberon.source.Span;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public sealed class RedNode {

    private final RedNode parent;
    private final GreenNode green;
    private final int startOffset;

    protected RedNode(final RedNode parent, final GreenNode green, final int startOffset) {
        this.parent = parent;
        this.green = green;
        this.startOffset = startOffset;
    }

    public RedNode parent() { return parent; }
    public GreenNode green() { return green; }
    public SyntaxKind kind() { return green.kind(); }
    public int startOfset() { return startOffset; }
    public int textLength() { return green.textLength(); }
    public Span span() { return new Span(startOffset, textLength()); }

    public static RedNode createRoot(final GreenNode greenRoot) {
        if (null == greenRoot) {
            return null;
        }
        return create(null, greenRoot, 0);
    }

    public RedNode findNodeAtOffset(final int offset) {
        if (offset < startOffset || offset >= startOffset + textLength()) {
            return null;
        }
        if (this instanceof TokenLeaf) {
            return this;
        }
        if (this instanceof Composite composite) {
            for (RedNode child : composite.children) {
                if (offset >= child.startOffset && offset < child.startOffset + child.textLength()) {
                    return child.findNodeAtOffset(offset);
                }
            }
        }
        return this;
    }

    public static final class TokenLeaf extends RedNode {
        private TokenLeaf(final RedNode parent, final GreenNode.TokenLeaf green, final int startOffset) {
            super(parent, green, startOffset);
        }
        public GreenNode.TokenLeaf tokenLeaf() {
            return (GreenNode.TokenLeaf) green();
        }
    }

    public static final class Composite extends RedNode {

        private final List<RedNode> children;
        private Composite(final RedNode parent, final GreenNode.Composite green, int startOffset) {
            super(parent, green, startOffset);
            List<RedNode> childList = new ArrayList<> (green.children().size());
            int currentOffset = startOffset;
            for (GreenNode childGreen : green.children()) {
                RedNode childRed = RedNode.create(this, childGreen, currentOffset);
                childList.add(childRed);
                currentOffset += childGreen.textLength();
            }
            this.children = Collections.unmodifiableList(childList);
        }

        public List<RedNode> children() { return children; }
    }

    private static RedNode create(final RedNode parent, final GreenNode green, final int startOffset) {
        if (green instanceof GreenNode.TokenLeaf leaf) {
            return new TokenLeaf(parent, leaf, startOffset);
        } else if (green instanceof GreenNode.Composite composite) {
            return new Composite(parent, composite, startOffset);
        }
        throw new IllegalArgumentException("Unknown green node type: " + green.getClass());
    }
}
