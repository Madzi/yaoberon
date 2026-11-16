package io.github.madzi.yaoberon.format.internal;

import io.github.madzi.yaoberon.format.FormatFailureException;
import io.github.madzi.yaoberon.format.FormatResult;
import io.github.madzi.yaoberon.format.Formatter;
import io.github.madzi.yaoberon.lexer.Lexer;
import io.github.madzi.yaoberon.lexer.ScanResult;
import io.github.madzi.yaoberon.lexer.token.Token;
import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.lexer.token.Trivia;
import io.github.madzi.yaoberon.parser.ParseContext;
import io.github.madzi.yaoberon.parser.ParseResult;
import io.github.madzi.yaoberon.parser.Parser;
import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import io.github.madzi.yaoberon.source.SourceFile;
import io.github.madzi.yaoberon.source.Span;

public class BaseFormatter implements Formatter {
    

    private final String indentStep;
    private boolean isAtStartOfLine = true;

    public BaseFormatter(final String indentStep) {
        this.indentStep = indentStep;
    }

    @Override
    public FormatResult format(final String text) {
        SourceFile source = SourceFile.create("mem://temporal", 0, text);
        Lexer lexer = Lexer.create();
        ScanResult scan = lexer.tokenizeAll(source);
        Parser parser = Parser.create();
        ParseResult parse = parser.parse(new ParseContext(source, scan.tokens()));
        if (null == parse || null == parse.root()) {
            return new FormatResult.ParseFailed();
        }
        if (null != parse.syntaxErrors() && !parse.syntaxErrors().isEmpty()) {
            return new FormatResult.SyntaxErrors();
        }
        return new FormatResult.Successful(format(source, parse.root()));
    }

    public String format(final SourceFile source, final GreenNode root) {
        StringBuilder sb = new StringBuilder();
        CharSequence text = source.text();
        formatNode(text, root, sb, 0);
        return sb.toString();
    }

    private void formatNode(final CharSequence text, final GreenNode node, final StringBuilder sb, final int indentLevel) {
        if (node != null) {
        } else {
            return;
        }
        switch (node) {
            case GreenNode.TokenLeaf leaf ->
                formatTokenLeaf(text, leaf.token(), sb, indentLevel);
            case GreenNode.Composite composite -> {
                SyntaxKind kind = composite.kind();
                int childIndent = isIndentBlock(kind) ? indentLevel + 1 : indentLevel;

                for (GreenNode child : composite.children()) {
                    if (shouldAddNewlineBefore(child.kind())) {
                        ensureNewline(sb);
                    }
                    formatNode(text, child, sb, childIndent);
                }
            }
            default -> throw new IllegalStateException("Unexpected value: " + node);
        }
    }

    private void formatTokenLeaf(final CharSequence text, final Token token, final StringBuilder sb, final int indentLevel) {
        TokenKind kind = token.kind();
        for (Trivia trivia : token.trivias()) {
            if (trivia instanceof Trivia.Comment comment) {
                ensureIndent(sb, indentLevel);
                sb.append(extractText(text, comment.span()));
                ensureNewline(sb);
            }
        }
        if (kind == TokenKind.END || kind == TokenKind.ELSE || kind == TokenKind.ELSIF || kind == TokenKind.UNTIL) {
            ensureNewline(sb);
        }
        ensureIndent(sb, indentLevel);
        addLeadingSpaceIfNeeded(kind, sb);
        sb.append(extractText(text, token.span()));
        isAtStartOfLine = false;
        applyPostTokenFormatting(kind, sb);
    }

    private void addLeadingSpaceIfNeeded(final TokenKind kind, final StringBuilder sb) {
        if (isAtStartOfLine || sb.isEmpty()) {
            return;
        }
        char lastChar = sb.charAt(sb.length() - 1);
        if (lastChar == ' ' || lastChar == '\n' || lastChar == '(') {
            return;
        }
        if (kind == TokenKind.TIMES && (lastChar >= 'a' && lastChar <= 'z' || lastChar >= 'A' && lastChar <= 'Z' || lastChar >= '0' && lastChar <= '9')) {
            return;
        }
        switch (kind) {
            case OF, THEN, DO, BECOMES, EQL, NEQ, LSS, LEQ, GTR, GEQ, PLUS, MINUS, TIMES, RDIV, DIV, MOD, OR, AND, IN, IS, ELSIF, UNTIL, ELSE, RECORD, ARRAY -> {
                sb.append(" ");
            }
            default -> {
            }
        }
    }

    private void applyPostTokenFormatting(final TokenKind kind, final StringBuilder sb) {
        switch (kind) {
            case SEMICOLON, BEGIN, THEN, DO, ELSE ->
                ensureNewline(sb);

            case COMMA, COLON, BECOMES, EQL, NEQ, LSS, LEQ, GTR, GEQ, PLUS, MINUS, RDIV, DIV, MOD, OR, AND, OF, TO, BY, IMPORT, MODULE, PROCEDURE, VAR, CONST, TYPE, RETURN, ARRAY, END, REPEAT, IF, WHILE, FOR, CASE, ELSIF, UNTIL, RECORD -> {
                sb.append(" ");
            }
            default -> {
            }
        }
    }

    private String extractText(final CharSequence sourceText, final Span span) {
        return sourceText.subSequence(span.start(), span.start() + span.length()).toString();
    }

    private void ensureIndent(final StringBuilder sb, final int indentLevel) {
        if (isAtStartOfLine) {
            sb.append(indentStep.repeat(Math.max(0, indentLevel)));
            isAtStartOfLine = false;
        }
    }

    private void ensureNewline(final StringBuilder sb) {
        if (!isAtStartOfLine) {
            while (!sb.isEmpty() && sb.charAt(sb.length() - 1) == ' ') {
                sb.setLength(sb.length() - 1);
            }
            sb.append("\n");
            isAtStartOfLine = true;
        }
    }

    private boolean isIndentBlock(final SyntaxKind kind) {
        return switch (kind) {
            case CONST_SECTION, TYPE_SECTION, VAR_SECTION, RECORD_TYPE, PROCEDURE_BODY, STATEMENT_SEQUENCE ->
                true;
            default ->
                false;
        };
    }

    private boolean shouldAddNewlineBefore(final SyntaxKind kind) {
        return switch (kind) {
            case CONST_SECTION, TYPE_SECTION, VAR_SECTION, PROCEDURE_DECLARATION ->
                true;
            default ->
                false;
        };
    }
}
