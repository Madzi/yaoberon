package io.github.madzi.yaoberon.lsp.service.internal;

import io.github.madzi.yaoberon.source.SourceFile;
import io.github.madzi.yaoberon.source.Span;

import java.util.Arrays;
import java.util.List;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.TextEdit;

public class LspSourceFile implements SourceFile {

    private final SourceFile delegate;
    private final int[] lineStarts;

    public LspSourceFile(final SourceFile delegate) {
        this.delegate = delegate;
        this.lineStarts = buildLineStarts(delegate.text().toString());
    }

    @Override
    public String uri() {
        return delegate.uri();
    }

    @Override
    public int version() {
        return delegate.version();
    }

    @Override
    public CharSequence text() {
        return delegate.text();
    }

    @Override
    public int length() {
        return delegate.length();
    }

    @Override
    public CharSequence piece(final Span span) {
        return delegate.piece(span);
    }

    public int nextNewLine(final int offset) {
        int idx = 0;
        while (idx < lineStarts.length && offset > lineStarts[idx]) {
            idx++;
        }
        return lineStarts[idx];
    }

    public LspSourceFile applyEdits(final int newVersion, final List<TextEdit> edits) {
        return applyChanges(newVersion, edits.stream().map(edit -> new Change(edit.getRange(), edit.getNewText())).toList());
    }

    public LspSourceFile applyLspEvents(final int newVersion, final List<TextDocumentContentChangeEvent> events) {
        return applyChanges(newVersion, events.stream().map(event -> new Change(event.getRange(), event.getText())).toList());
    }

    public Position offsetToPosition(final int offset) {
        int idx = Arrays.binarySearch(lineStarts, offset);
        if (idx < 0) {
            idx = -idx - 2;
        }
        idx = Math.max(0, Math.min(idx, lineStarts.length - 1));
        final int lineStart = lineStarts[idx];
        return new Position(idx, offset - lineStart);
    }

    public int positionToOffset(final Position position) {
        if (position.getLine() >= lineStarts.length) {
            return delegate.length();
        }
        final int lineStart = lineStarts[position.getLine()];
        return Math.min(lineStart + position.getCharacter(), delegate.length());
    }

    private LspSourceFile applyChanges(final int newVersion, final List<Change> changes) {
        final StringBuilder builder = new StringBuilder(delegate.text());
        for (Change change : changes) {
            Range range = change.range();
            if (null == range) {
                return create(delegate.uri(), newVersion, delegate.text());
            }
            int startOffset = positionToOffset(range.getStart());
            int endOffset = positionToOffset(range.getEnd());
            builder.replace(startOffset, endOffset, change.text());
        }
        return create(delegate.uri(), newVersion, builder.toString());
    }

    public static LspSourceFile create(final String uri, final int version, final CharSequence content) {
        return new LspSourceFile(SourceFile.create(uri, version, content.toString()));
    }

    private static int[] buildLineStarts(final String text) {
        int count = 1;
        for (int i = 0; i < text.length(); ++i) {
            if ('\n' == text.charAt(i)) {
                count++;
            }
        }
        int[] starts = new int[count];
        starts[0] = 0;
        int idx = 1;
        for (int i = 0; i < text.length(); ++i) {
            if ('\n' == text.charAt(i)) {
                starts[idx++] = i + 1;
            }
        }
        return starts;
    }

    private static record Change(Range range, String text) {}
}
