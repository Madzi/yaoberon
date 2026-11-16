package io.github.madzi.yaoberon.format;

public sealed interface FormatResult {

    record Successful(String formatted) implements FormatResult {}
    record ParseFailed() implements FormatResult {}
    record SyntaxErrors() implements FormatResult {}
}
