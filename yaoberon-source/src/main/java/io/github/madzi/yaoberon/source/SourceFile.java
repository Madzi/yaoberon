package io.github.madzi.yaoberon.source;

import io.github.madzi.yaoberon.source.internal.BaseSourceFile;

public interface SourceFile {

    String uri();
    int version();
    CharSequence text();
    int length();
    CharSequence piece(Span span);
    static SourceFile create(final String uri, final int version, final String content) {
        return new BaseSourceFile(uri, version, content);
    }
}
