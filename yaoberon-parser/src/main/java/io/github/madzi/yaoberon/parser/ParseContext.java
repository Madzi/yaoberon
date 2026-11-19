package io.github.madzi.yaoberon.parser;

import io.github.madzi.yaoberon.diagnostic.Diagnostic;
import io.github.madzi.yaoberon.lexer.token.Token;
import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import io.github.madzi.yaoberon.source.SourceFile;
import io.github.madzi.yaoberon.source.Span;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ParseContext {

    private final List<Token> tokens;
    private final List<Diagnostic> diagnostics;
    private final AtomicInteger cursor;
    private final SourceFile source;

    public ParseContext(final SourceFile source, final List<Token> tokens) {
        this.source = source;
        this.tokens = List.copyOf(tokens);
        this.diagnostics = new ArrayList<>();
        this.cursor = new AtomicInteger(0);
    }

    public List<Token> tokens() { return tokens; }
    public List<Diagnostic> diagnostics() { return diagnostics; }
    public AtomicInteger cursor() { return cursor; }
    public SourceFile source() { return source; }
    public Token peek() { return tokens.get(cursor.get()); }
    public Token next() { return tokens.get(cursor.getAndIncrement()); }
    public boolean check(final TokenKind kind) {
        if (isAtEnd()) { return false; }
        return peek().kind() == kind;
    }
    public boolean inList(final TokenKind... kinds) {
        if (isAtEnd()) { return false; }
        return Arrays.asList(kinds).contains(peek().kind());
    }
    public boolean isAtEnd() { return cursor.get() >= tokens.size() || peek().isEot(); }
    public GreenNode consume(final SyntaxKind kind) { return new GreenNode.TokenLeaf(kind, next()); }
    public void reportError(final String message, final Object... params) {
        final Token current = isAtEnd() ? tokens.getLast() : peek();
        diagnostics.add(new Diagnostic(Diagnostic.Severity.ERROR, current.span(), message, params));
    }
    public String getSourceText() {
        final Span span = peek().span();
        return source.text().subSequence(span.start(), span.end()).toString();
    }
    public boolean isNextTokenIdentifier() {
        if (cursor.get() + 1 < tokens.size()) {
            return tokens.get(cursor.get() + 1).kind() == TokenKind.IDENT;
        }
        return false;
    }
    public boolean isTypeGuard() {
        if (cursor.get() + 1 < tokens.size() && tokens.get(cursor.get() + 1).kind() == TokenKind.IDENT) {
            int idx = cursor.get() + 2;
            if (idx < tokens.size() && tokens.get(idx).kind() == TokenKind.PERIOD) {
                idx += 2;
            }
            if (idx < tokens.size() && tokens.get(idx).kind() == TokenKind.RPAREN) {
                int nextIdx = idx + 1;
                if (nextIdx < tokens.size()) {
                    TokenKind next = tokens.get(nextIdx).kind();
                    return next == TokenKind.PERIOD || next == TokenKind.LBRAK || next == TokenKind.ARROW;
                }
            }
        }
        return false;
    }
}
