package io.github.madzi.yaoberon.lexer.token;

import io.github.madzi.yaoberon.source.Span;

public sealed interface Trivia {
    Span span();

    record Whitespace(Span span) implements Trivia {}
    record Comment(Span span, boolean isNestedCorrectly) implements Trivia {}
}
