package io.github.madzi.yaoberon.lsp.service;

import io.github.madzi.yaoberon.lsp.YaOberonLanguageServer;
import io.github.madzi.yaoberon.lsp.service.internal.FoldingRangeCollector;
import io.github.madzi.yaoberon.lsp.service.internal.LspSourceFile;
import io.github.madzi.yaoberon.lsp.service.internal.SourcesStorage;
import io.github.madzi.yaoberon.source.SourceFile;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
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

    private void recompileAndPublishDiagnostics(final LspSourceFile source) {
        server.client().ifPresent(client -> client.publishDiagnostics(new PublishDiagnosticsParams(source.uri(), openFiles.recompileForDiagnostics(source))));
    }
}
