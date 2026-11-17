package io.github.madzi.yaoberon.lsp.service;

import io.github.madzi.yaoberon.lexer.token.Token;
import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.lexer.token.Trivia;
import io.github.madzi.yaoberon.lsp.YaOberonLanguageServer;
import io.github.madzi.yaoberon.lsp.service.internal.FoldingRangeCollector;
import io.github.madzi.yaoberon.lsp.service.internal.LspSourceFile;
import io.github.madzi.yaoberon.lsp.service.internal.SourcesStorage;
import io.github.madzi.yaoberon.source.Span;
import io.github.madzi.yaoberon.symbol.RedNode;
import io.github.madzi.yaoberon.symbol.Symbol;
import io.github.madzi.yaoberon.symbol.SymbolResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.SemanticTokens;
import org.eclipse.lsp4j.SemanticTokensParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.services.TextDocumentService;

public class YaOberonTextDocumentService implements TextDocumentService {

    private final YaOberonLanguageServer server;
    private final SourcesStorage openFiles;
    private final FoldingRangeCollector rangeCollector;

    public YaOberonTextDocumentService(final YaOberonLanguageServer server) {
        this.server = server;
        this.openFiles = new SourcesStorage();
        this.rangeCollector = new FoldingRangeCollector();
    }

    @Override
    public void didOpen(final DidOpenTextDocumentParams params) {
        var source = openFiles.put(params.getTextDocument());
        recompileAndPublishDiagnostics(source);
    }

    @Override
    public void didChange(final DidChangeTextDocumentParams params) {
        VersionedTextDocumentIdentifier doc = params.getTextDocument();
        String newText = params.getContentChanges().get(0).getText();
        int newVersion = doc.getVersion();
        openFiles.get(doc.getUri()).ifPresentOrElse(
                source -> {
                    openFiles.put(source.applyLspEvents(newVersion, params.getContentChanges()));
                    Thread.startVirtualThread(() -> recompileAndPublishDiagnostics(source));
                },
                () -> openFiles.put(LspSourceFile.create(doc.getUri(), newVersion, newText))
        );
    }

    @Override
    public void didClose(final DidCloseTextDocumentParams params) {
        TextDocumentIdentifier doc = params.getTextDocument();
        openFiles.remove(doc.getUri());
        server.client().ifPresent(client -> client.publishDiagnostics(new PublishDiagnosticsParams(doc.getUri(), List.of())));
    }

    @Override
    public void didSave(final DidSaveTextDocumentParams params) {
        TextDocumentIdentifier doc = params.getTextDocument();
        openFiles.get(doc.getUri()).ifPresent(this::recompileAndPublishDiagnostics);
    }

    @Override
    public CompletableFuture<List<FoldingRange>> foldingRange(final FoldingRangeRequestParams params) {
        var optSource = openFiles.get(params.getTextDocument().getUri());
        if (optSource.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        var source = optSource.get();
        var parseResult = openFiles.parse(source);
        if (null == parseResult.root()) {
            return CompletableFuture.completedFuture(List.of());
        }
        List<FoldingRange> foldingRanges = rangeCollector.collect(source, parseResult.root());
        return CompletableFuture.completedFuture(foldingRanges);
    }

    @Override
    public CompletableFuture<SemanticTokens> semanticTokensFull(final SemanticTokensParams params) {
        var doc = params.getTextDocument();
        var optSource = openFiles.get(doc.getUri());
        if (optSource.isEmpty()) {
            return CompletableFuture.completedFuture(new SemanticTokens(List.of()));
        }
        var source = optSource.get();
        var scanResult = openFiles.scan(source);
        var parseResult = openFiles.parse(source);
        if (null == parseResult.root()) {
            return CompletableFuture.completedFuture(new SemanticTokens(List.of()));
        }
        RedNode root = RedNode.createRoot(parseResult.root());
        SymbolResolver resolver = SymbolResolver.create(source);
        resolver.resolve(root);
        List<Integer> data = new ArrayList<>();
        var tracker = new PositionTracker();
        for (Token token : scanResult.tokens()) {
            for (Trivia trivia : token.trivias()) {
                emitTrivia(trivia, source, tracker, data);
            }
            int tokenType = getSemanticTokenType(token, root, resolver);
            if (tokenType >= 0) {
                emitSpan(token.span(), tokenType, source, tracker, data);
            }
        }
        return CompletableFuture.completedFuture(new SemanticTokens(data));
    }

    private void emitTrivia(final Trivia trivia, final LspSourceFile source, final PositionTracker tracker, final List<Integer> data) {
        if (trivia instanceof Trivia.Comment comment) {
            int commentTokenType = 5;
            emitMultilineSpan(comment.span(), commentTokenType, source, tracker, data);
        }
    }

    private void emitSpan(final Span span, final int tokenType, final LspSourceFile source, final PositionTracker tracker, final List<Integer> data) {
        Position pos = source.offsetToPosition(span.start());
        addTokenData(pos.getLine(), pos.getCharacter(), span.length(), tokenType, tracker, data);
    }

    private void emitMultilineSpan(final Span span, final int tokenType, final LspSourceFile source, final PositionTracker tracker, final List<Integer> data) {
        Position startPos = source.offsetToPosition(span.start());
        Position endPos = source.offsetToPosition(span.end());
        if (startPos.getLine() == endPos.getLine()) {
            addTokenData(startPos.getLine(), startPos.getCharacter(), span.length(), tokenType, tracker, data);
            return;
        }
        CharSequence text = source.text();
        int currentOffset = span.start();
        int remainingLength = span.length();
        while (remainingLength > 0 && currentOffset < text.length()) {
            Position pos = source.offsetToPosition(currentOffset);
            int lineEndOffset = getLineEndOffset(text, currentOffset);
            int lineChunkLength = Math.min(remainingLength, lineEndOffset - currentOffset);
            if (lineChunkLength > 0) {
                addTokenData(pos.getLine(), pos.getCharacter(), lineChunkLength, tokenType, tracker, data);
            }
            int nextOffset = lineEndOffset;
            while (nextOffset < text.length() && (text.charAt(nextOffset) == '\r' || text.charAt(nextOffset) == '\n')) {
                nextOffset++;
            }
            int step = nextOffset - currentOffset;
            if (0 == step) {
                step = 1;
            }
            remainingLength -= step;
            currentOffset = nextOffset;
        }
    }

    private void addTokenData(int line, int col, int length, int tokenType, PositionTracker tracker, List<Integer> data) {
        int deltaLine = line - tracker.lastLine;
        int deltaStart = (deltaLine == 0) ? (col - tracker.lastCol) : col;
        data.add(deltaLine);
        data.add(deltaStart);
        data.add(length);
        data.add(tokenType);
        data.add(0);
        tracker.lastLine = line;
        tracker.lastCol = col;
    }

    private int getLineEndOffset(CharSequence text, int startOffset) {
        int idx = startOffset;
        while (idx < text.length()) {
            char ch = text.charAt(idx);
            if (ch == '\r' || ch == '\n') {
                break;
            }
            idx++;
        }
        return idx;
    }

    private int getSemanticTokenType(TokenKind kind) {
        return switch (kind) {
            case MODULE, IMPORT, CONST, TYPE, VAR, BEGIN, END,
            ARRAY, OF, RECORD, POINTER, TO, PROCEDURE, RETURN,
            WHILE, IF, THEN, ELSIF, ELSE, FOR, BY, DO,
            REPEAT, UNTIL -> 0;
            case IDENT -> 1;
            case INT, REAL, CHAR -> 2;
            case SEMICOLON, PERIOD, EQL, NEQ, LSS, LEQ, GTR, GEQ, COLON, COMMA, BECOMES,
            PLUS, MINUS, TIMES, RDIV, DIV, MOD, LPAREN, RPAREN -> 3;
            case STRING -> 4;
            default -> -1;
        };
    }

    private int getSemanticTokenType(final Token token, final RedNode redRoot, final SymbolResolver resolver) {
        TokenKind kind = token.kind();
        int semKind = getSemanticTokenType(kind);
        if (semKind >= 0) {
            return semKind;
        }
        if (TokenKind.IDENT == kind) {
            int tokenStart = token.span().start();
            RedNode targetNode = redRoot.findNodeAtOffset(tokenStart);
            var optSymbol = resolver.findSymbolFor(targetNode);
            if (optSymbol.isPresent()) {
                Symbol symbol = optSymbol.get();
                return switch (symbol.kind()) {
                    case TYPE -> 6;
                    case PROCEDURE -> 7;
                    case VARIABLE, PARAMETER, CONSTANT -> 1;
                    case MODULE -> 0;
                };
            }
            return 1;
        }
        return -1;
    }

    private void recompileAndPublishDiagnostics(final LspSourceFile source) {
        server.client().ifPresent(client -> client.publishDiagnostics(new PublishDiagnosticsParams(source.uri(), openFiles.recompileForDiagnostics(source))));
    }

    private static class PositionTracker {
        int lastLine = 0;
        int lastCol = 0;
    }
}
