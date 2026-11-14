package io.github.madzi.yaoberon.parser;

import io.github.madzi.yaoberon.source.Span;

public record SyntaxError(Span span, String message) {
}
