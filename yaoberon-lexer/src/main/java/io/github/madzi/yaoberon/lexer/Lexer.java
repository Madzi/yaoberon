package io.github.madzi.yaoberon.lexer;

import io.github.madzi.yaoberon.lexer.internal.BaseLexer;
import io.github.madzi.yaoberon.source.SourceFile;

public interface Lexer {
    default ScanResult tokenizeAll(final SourceFile source) {
        return tokenizeRange(source, 0, source.length(), LexerState.INITIAL);
    }
    ScanResult tokenizeRange(SourceFile source, int startOffset, int endOffset, LexerState initialState);
    static Lexer create() { return new BaseLexer(); }
}
