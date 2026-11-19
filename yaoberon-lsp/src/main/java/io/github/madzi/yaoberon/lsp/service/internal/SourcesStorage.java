package io.github.madzi.yaoberon.lsp.service.internal;

import io.github.madzi.yaoberon.lexer.Lexer;
import io.github.madzi.yaoberon.lexer.ScanResult;
import io.github.madzi.yaoberon.parser.ParseContext;
import io.github.madzi.yaoberon.parser.ParseResult;
import io.github.madzi.yaoberon.parser.Parser;
import io.github.madzi.yaoberon.source.SourceFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.lsp4j.Diagnostic;
import org.eclipse.lsp4j.DiagnosticSeverity;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentItem;

public class SourcesStorage {

    private final Lexer lexer;
    private final Parser parser;
    private final Map<String, LspSourceFile> sources;

    public SourcesStorage() {
        lexer = Lexer.create();
        parser = Parser.create();
        sources = new ConcurrentHashMap<>();
    }

    public LspSourceFile put(final TextDocumentItem item) {
        return SourcesStorage.this.put(LspSourceFile.create(item.getUri(), item.getVersion(), item.getText()));
    }

    public LspSourceFile put(final LspSourceFile source) {
        return sources.put(source.uri(), source);
    }

    public LspSourceFile remove(final LspSourceFile source) {
        return SourcesStorage.this.remove(source.uri());
    }

    public LspSourceFile remove(final String uri) {
        return sources.remove(uri);
    }

    public Optional<LspSourceFile> get(final String uri) {
        return Optional.ofNullable(sources.get(uri));
    }

    public ScanResult scan(final LspSourceFile source) {
        return lexer.tokenizeAll(source);
    }

    public ParseResult parse(final LspSourceFile source) {
        var scanResult = scan(source);
        return parser.parse(new ParseContext(source, scanResult.tokens()));
    }

    public List<Diagnostic> recompileForDiagnostics(final LspSourceFile source) {
        var parseResult = parse(source);
        List<Diagnostic> diagnostics = new ArrayList<>();
        for (var diagnostic : parseResult.diagnostics()) {
            Position startPos = source.offsetToPosition(diagnostic.span().start());
            Position endPos = source.offsetToPosition(diagnostic.span().end());
            diagnostics.add(new Diagnostic(new Range(startPos, endPos), diagnostic.message(), DiagnosticSeverity.Error, "yao"));
        }
        return diagnostics;
    }
}
