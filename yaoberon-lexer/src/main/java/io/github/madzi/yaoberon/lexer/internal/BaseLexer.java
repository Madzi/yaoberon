package io.github.madzi.yaoberon.lexer.internal;

import io.github.madzi.yaoberon.lexer.Lexer;
import io.github.madzi.yaoberon.lexer.LexerState;
import io.github.madzi.yaoberon.lexer.ScanResult;
import io.github.madzi.yaoberon.lexer.token.Token;
import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.lexer.token.Trivia;
import io.github.madzi.yaoberon.source.SourceFile;
import io.github.madzi.yaoberon.source.Span;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BaseLexer implements Lexer {

    private final Map<String, TokenKind> keywords = defineKeywords();

    private boolean isHexDigit(final char ch) {
        return (ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F') || (ch >= 'a' && ch <= 'f');
    }

    @Override
    public ScanResult tokenizeRange(final SourceFile source, final int startOffset, final int endOffset, final LexerState initialState) {
        CharSequence fullText = source.text();
        int limit = Math.min(endOffset, fullText.length());
        List<Token> tokens = new ArrayList<>();
        List<Trivia> trivias = new ArrayList<>();
        int idx = startOffset;
        int commentsDeep = initialState.commentNestingDepth();
        while (idx < limit) {
            if (commentsDeep > 0 || (fullText.charAt(idx) == '(' && isNext(fullText, idx, '*', limit))) {
                int commentStart = idx;
                if (commentsDeep == 0) {
                    commentsDeep++;
                    idx += 2;
                }
                while (idx < limit && commentsDeep > 0) {
                    if (fullText.charAt(idx) == '(' && isNext(fullText, idx, '*', limit)) {
                        commentsDeep++;
                        idx += 2;
                    }
                    else if (fullText.charAt(idx) == '*' && isNext(fullText, idx, ')', limit)) {
                        commentsDeep--;
                        idx += 2;
                    }
                    else { idx++; }
                }
                boolean isClosed = (commentsDeep == 0);
                trivias.add(new Trivia.Comment(new Span(commentStart, idx - commentStart), isClosed));
                continue;
            }
            int start = idx;
            char ch = fullText.charAt(idx);
            if (Character.isWhitespace(ch)) {
                while (idx < limit && Character.isWhitespace(fullText.charAt(idx))) { idx++; }
                trivias.add(new Trivia.Whitespace(new Span(start, idx - start)));
            }
            else if (isIdentifierStart(ch)) {
                while (idx < limit && isIdentifierPart(fullText.charAt(idx))) { idx++; }
                String text = fullText.subSequence(start, idx).toString();
                TokenKind kind = keywords.getOrDefault(text, TokenKind.IDENT);
                tokens.add(new Token(kind, new Span(start, idx - start), trivias));
                trivias = new ArrayList<>();
            }
            else if (Character.isDigit(ch)) {
                int numStart = idx;
                while (idx < limit && isHexDigit(fullText.charAt(idx))) {
                    idx++;
                }
                if (idx < limit && (fullText.charAt(idx) == 'X' || fullText.charAt(idx) == 'x')) {
                    idx++;
                    tokens.add(new Token(TokenKind.STRING, new Span(start, idx - start), trivias));
                    trivias = new ArrayList<>();
                }
                else if (idx < limit && fullText.charAt(idx) == '.' && isNextDigit(fullText, idx, limit)) {
                    if (idx + 1 < limit && fullText.charAt(idx + 1) == '.') {
                        idx = numStart;
                        tokens.add(new Token(TokenKind.REAL, new Span(start, numStart - start), trivias));
                        trivias = new ArrayList<>();
                    } else {
                        idx++;
                        while (idx < limit && Character.isDigit(fullText.charAt(idx))) {
                            idx++;
                        }
                        if (idx < limit && (fullText.charAt(idx) == 'E' || fullText.charAt(idx) == 'D' || fullText.charAt(idx) == 'e' || fullText.charAt(idx) == 'd')) {
                            idx++;
                            if (idx < limit && (fullText.charAt(idx) == '+' || fullText.charAt(idx) == '-')) {
                                idx++;
                            }
                            while (idx < limit && Character.isDigit(fullText.charAt(idx))) {
                                idx++;
                            }
                        }
                        tokens.add(new Token(TokenKind.REAL, new Span(start, idx - start), trivias));
                        trivias = new ArrayList<>();
                    }
                }
                else {
                    if (idx < limit && (fullText.charAt(idx) == 'H' || fullText.charAt(idx) == 'h')) {
                        idx++;
                        tokens.add(new Token(TokenKind.INT, new Span(start, idx - start), trivias));
                    } else if (idx < limit && (fullText.charAt(idx) == 'R' || fullText.charAt(idx) == 'r')) {
                        idx++;
                        tokens.add(new Token(TokenKind.REAL, new Span(start, idx - start), trivias));
                    } else {
                        tokens.add(new Token(TokenKind.INT, new Span(start, idx - start), trivias));
                    }
                    trivias = new ArrayList<>();
                }
            }
            else if (ch == '"') {
                char quote = ch;
                idx++;
                while (idx < limit && fullText.charAt(idx) != quote && fullText.charAt(idx) != '\n' && fullText.charAt(idx) != '\r') {
                    idx++;
                }
                if (idx < limit && fullText.charAt(idx) == quote) {
                    idx++;
                }
                tokens.add(new Token(TokenKind.STRING, new Span(start, idx - start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '$') {
                idx++;
                while (idx < limit && fullText.charAt(idx) != '$') {
                    idx++;
                }
                if (idx < limit && fullText.charAt(idx) == '$') {
                    idx++;
                }
                tokens.add(new Token(TokenKind.STRING, new Span(start, idx - start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '\'') {
                idx++;
                while (idx < limit && fullText.charAt(idx) != '\'' && fullText.charAt(idx) != '\n' && fullText.charAt(idx) != '\r') {
                    idx++;
                }
                if (idx < limit && fullText.charAt(idx) == '\'') {
                    idx++;
                }
                tokens.add(new Token(TokenKind.STRING, new Span(start, idx - start), trivias));
                trivias = new ArrayList<>();
            }
            else if (ch == '=') {
                idx++;
                tokens.add(new Token(TokenKind.EQL, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '#') {
                idx++;
                tokens.add(new Token(TokenKind.NEQ, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '<') {
                if (isNext(fullText, idx, '=', limit)) {
                    idx += 2;
                    tokens.add(new Token(TokenKind.LEQ, new Span(start, 2), trivias));
                } else {
                    idx++;
                    tokens.add(new Token(TokenKind.LSS, oneChar(start), trivias));
                }
                trivias = new ArrayList<>();
            } else if (ch == '>') {
                if (isNext(fullText, idx, '=', limit)) {
                    idx += 2;
                    tokens.add(new Token(TokenKind.GEQ, new Span(start, 2), trivias));
                } else {
                    idx++;
                    tokens.add(new Token(TokenKind.GTR, oneChar(start), trivias));
                }
                trivias = new ArrayList<>();
            } else if (ch == ';') {
                idx++;
                tokens.add(new Token(TokenKind.SEMICOLON, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '.') {
                if (isNext(fullText, idx, '.', limit)) {
                    idx += 2;
                    tokens.add(new Token(TokenKind.UPTO, new Span(start, 2), trivias));
                } else {
                    idx++;
                    tokens.add(new Token(TokenKind.PERIOD, oneChar(start), trivias));
                }
                trivias = new ArrayList<>();
            } else if (ch == ':') {
                if (isNext(fullText, idx, '=', limit)) {
                    idx += 2;
                    tokens.add(new Token(TokenKind.BECOMES, new Span(start, 2), trivias));
                } else {
                    idx++;
                    tokens.add(new Token(TokenKind.COLON, oneChar(start), trivias));
                }
                trivias = new ArrayList<>();
            } else if (ch == ',') {
                idx++;
                tokens.add(new Token(TokenKind.COMMA, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '+') {
                idx++;
                tokens.add(new Token(TokenKind.PLUS, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '-') {
                idx++;
                tokens.add(new Token(TokenKind.MINUS, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '*') {
                idx++;
                tokens.add(new Token(TokenKind.TIMES, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '/') {
                idx++;
                tokens.add(new Token(TokenKind.RDIV, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '(') {
                idx++;
                tokens.add(new Token(TokenKind.LPAREN, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == ')') {
                idx++;
                tokens.add(new Token(TokenKind.RPAREN, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '[') {
                idx++;
                tokens.add(new Token(TokenKind.LBRAK, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == ']') {
                idx++;
                tokens.add(new Token(TokenKind.RBRAK, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '{') {
                idx++;
                tokens.add(new Token(TokenKind.LBRACE, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '}') {
                idx++;
                tokens.add(new Token(TokenKind.RBRACE, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '^' || ch == '↑') {
                idx++;
                tokens.add(new Token(TokenKind.ARROW, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '~') {
                idx++;
                tokens.add(new Token(TokenKind.NOT, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '&') {
                idx++;
                tokens.add(new Token(TokenKind.AND, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '|') {
                idx++;
                tokens.add(new Token(TokenKind.BAR, oneChar(start), trivias));
                trivias = new ArrayList<>();
            } else if (ch == '7' && idx + 2 < limit && fullText.charAt(idx + 1) == 'F' && fullText.charAt(idx + 2) == 'X') {
                idx += 3;
                tokens.add(new Token(TokenKind.UPTO, new Span(start, 3), trivias));
                trivias = new ArrayList<>();
            } else {
                idx++;
                tokens.add(new Token(TokenKind.ERROR, oneChar(start), trivias));
                trivias = new ArrayList<>();
            }
        }
        if (idx == limit) {
            tokens.add(new Token(TokenKind.EOT, new Span(limit, 0), trivias));
        }
        return new ScanResult(tokens, new LexerState(commentsDeep), idx);
    }

    private Span oneChar(final int start) {
        return new Span(start, 1);
    }

    private boolean isNext(CharSequence text, int currentIdx, char expected, int limit) {
        return (currentIdx + 1 < limit) && text.charAt(currentIdx + 1) == expected;
    }

    private boolean isNextDigit(CharSequence text, int currentIdx, int limit) {
        return (currentIdx + 1 < limit) && Character.isDigit(text.charAt(currentIdx + 1));
    }

    private boolean isIdentifierStart(char ch) {
        return ('A' <= ch && ch <= 'Z') || ('a' <= ch && ch <= 'z');
    }

    private boolean isIdentifierPart(char ch) {
        return isIdentifierStart(ch) || ('0' <= ch && ch <= '9') || ch == '_';
    }

    private static Map<String, TokenKind> defineKeywords() {
        final Map<String, TokenKind> keywords = new HashMap<>();
        keywords.put("IF", TokenKind.IF);
        keywords.put("DO", TokenKind.DO);
        keywords.put("OF", TokenKind.OF);
        keywords.put("OR", TokenKind.OR);
        keywords.put("TO", TokenKind.TO);
        keywords.put("IN", TokenKind.IN);
        keywords.put("IS", TokenKind.IS);
        keywords.put("BY", TokenKind.BY);
        keywords.put("END", TokenKind.END);
        keywords.put("NIL", TokenKind.NIL);
        keywords.put("VAR", TokenKind.VAR);
        keywords.put("DIV", TokenKind.DIV);
        keywords.put("MOD", TokenKind.MOD);
        keywords.put("FOR", TokenKind.FOR);
        keywords.put("ELSE", TokenKind.ELSE);
        keywords.put("THEN", TokenKind.THEN);
        keywords.put("TRUE", TokenKind.TRUE);
        keywords.put("TYPE", TokenKind.TYPE);
        keywords.put("CASE", TokenKind.CASE);
        keywords.put("ELSIF", TokenKind.ELSIF);
        keywords.put("FALSE", TokenKind.FALSE);
        keywords.put("ARRAY", TokenKind.ARRAY);
        keywords.put("BEGIN", TokenKind.BEGIN);
        keywords.put("CONST", TokenKind.CONST);
        keywords.put("UNTIL", TokenKind.UNTIL);
        keywords.put("WHILE", TokenKind.WHILE);
        keywords.put("RECORD", TokenKind.RECORD);
        keywords.put("REPEAT", TokenKind.REPEAT);
        keywords.put("RETURN", TokenKind.RETURN);
        keywords.put("IMPORT", TokenKind.IMPORT);
        keywords.put("MODULE", TokenKind.MODULE);
        keywords.put("POINTER", TokenKind.POINTER);
        keywords.put("PROCEDURE", TokenKind.PROCEDURE);
        return keywords;
    }
}
