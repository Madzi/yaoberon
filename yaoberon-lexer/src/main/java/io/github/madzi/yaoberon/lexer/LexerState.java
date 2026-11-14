package io.github.madzi.yaoberon.lexer;

public record LexerState(int commentNestingDepth) {
    public static final LexerState INITIAL = new LexerState(0);
}
