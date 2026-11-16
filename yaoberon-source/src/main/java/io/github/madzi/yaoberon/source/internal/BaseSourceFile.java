package io.github.madzi.yaoberon.source.internal;

import io.github.madzi.yaoberon.source.SourceFile;

public class BaseSourceFile implements SourceFile {

    private final String uri;
    private final int version;
    private final String content;

    public BaseSourceFile(final String uri, final int version, final String content) {
        this.uri = uri;
        this.version = version;
        this.content = content;
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
}
