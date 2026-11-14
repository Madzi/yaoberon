package io.github.madzi.yaoberon.source;

public interface SourceFile {

    String uri();
    int version();
    CharSequence text();
    int length();
}
