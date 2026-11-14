package io.github.madzi.yaoberon.lexer;

import io.github.madzi.yaoberon.lexer.token.Token;
import java.util.List;

public record ScanResult(List<Token> tokens, LexerState finalState, int stoppedAtOffset) {
}
