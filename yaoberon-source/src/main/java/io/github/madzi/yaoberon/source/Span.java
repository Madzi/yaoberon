package io.github.madzi.yaoberon.source;

public record Span(int start, int length) {
    public int end() { return start + length; }
}
