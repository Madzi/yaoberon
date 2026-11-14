package io.github.madzi.yaoberon.source.internal;

import io.github.madzi.yaoberon.source.SourceFile;

public class BaseSourceFile implements SourceFile {

    private final String uri;
    private final int version;
    private final String content;
    private final int[] lineStarts;

    public BaseSourceFile(final String uri, final int version, final String content) {
        this.uri = uri;
        this.version = version;
        this.content = content;
        this.lineStarts = buildLineStarts(content);
    }

    @Override
    public String uri() {
        return uri;
    }

    @Override
    public int version() {
        return version;
    }

    @Override
    public CharSequence text() {
        return content;
    }

    @Override
    public int length() {
        return content.length();
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
}
