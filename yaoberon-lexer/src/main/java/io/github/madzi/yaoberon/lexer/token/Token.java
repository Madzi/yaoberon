package io.github.madzi.yaoberon.lexer.token;

import io.github.madzi.yaoberon.source.Span;
import java.util.List;

public record Token(TokenKind kind, Span span, List<Trivia> trivias) {
    public Token { trivias = (null == trivias) ? List.of() : List.copyOf(trivias); }
    public boolean is(final TokenKind expected) { return expected == this.kind; }
    public boolean isEot() { return TokenKind.EOT == this.kind; }
}
