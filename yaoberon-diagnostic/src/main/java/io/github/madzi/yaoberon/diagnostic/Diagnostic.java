package io.github.madzi.yaoberon.diagnostic;

import io.github.madzi.yaoberon.source.Span;

public record Diagnostic(Severity severity, Span span, String message, Object... params) {

    public enum Severity {
        ERROR,
        WARNING,
        INFO,
        HINT
    }
}
