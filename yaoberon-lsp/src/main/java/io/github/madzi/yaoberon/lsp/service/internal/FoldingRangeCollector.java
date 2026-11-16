package io.github.madzi.yaoberon.lsp.service.internal;

import io.github.madzi.yaoberon.lexer.token.Trivia;
import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeKind;
import org.eclipse.lsp4j.Position;

public class FoldingRangeCollector {

    private static final List<SyntaxKind> FOLDABLE = List.of(
            SyntaxKind.MODULE_DECLARATION,
            SyntaxKind.CONST_SECTION,
            SyntaxKind.TYPE_SECTION,
            SyntaxKind.VAR_SECTION,
            SyntaxKind.RECORD_TYPE,
            SyntaxKind.PROCEDURE_DECLARATION,
            SyntaxKind.REPEAT_STATEMENT,
            SyntaxKind.WHILE_STATEMENT,
            SyntaxKind.FOR_STATEMENT,
            SyntaxKind.IF_STATEMENT
    );

    public List<FoldingRange> collect(final LspSourceFile source, final GreenNode root) {
        List<FoldingRange> ranges = new ArrayList<>();
        collectRecursive(root, 0, source, ranges);
        return ranges;
    }

    private void collectRecursive(final GreenNode node, final int nodeOffset, final LspSourceFile source, final List<FoldingRange> ranges) {
        if (node instanceof GreenNode.Composite composite) {
            SyntaxKind kind = composite.kind();
            if (FOLDABLE.contains(kind)) {
                int startOffset = getFirstTokenOffset(composite, nodeOffset);
                int endOffset = nodeOffset + composite.textLength();
                Position startPos = source.offsetToPosition(startOffset);
                Position endPos = source.offsetToPosition(endOffset);
                if (startPos.getLine() < endPos.getLine()) {
                    FoldingRange range = new FoldingRange(startPos.getLine(), endPos.getLine());
                    range.setKind(FoldingRangeKind.Region); //TODO: choose proper kind
                    ranges.add(range);
                }
            }
            int currentChildOffset = nodeOffset;
            for (GreenNode child : composite.children()) {
                collectRecursive(child, currentChildOffset, source, ranges);
                currentChildOffset += child.textLength();
            }
        } else if (node instanceof GreenNode.TokenLeaf leaf) {
            for (var trivia : leaf.token().trivias()) {
                if (trivia instanceof Trivia.Comment comment) {
                    Position startPos = source.offsetToPosition(comment.span().start());
                    Position endPos = source.offsetToPosition(comment.span().end());
                    if (startPos.getLine() < endPos.getLine()) {
                        FoldingRange range = new FoldingRange(startPos.getLine(), endPos.getLine());
                        range.setKind(FoldingRangeKind.Comment);
                        ranges.add(range);
                    }
                }
            }
        }
    }

    private int getFirstTokenOffset(final GreenNode node, final int currentOffset) {
        return switch (node) {
            case GreenNode.TokenLeaf leaf -> leaf.token().span().start();
            case GreenNode.Composite composite -> {
                int offset = currentOffset;
                for (GreenNode child : composite.children()) {
                    if (child.textLength() > 0) {
                        yield getFirstTokenOffset(child, offset);
                    }
                    offset += child.textLength();
                }
                yield currentOffset;
            }
            default -> currentOffset;
        };
    }
}
