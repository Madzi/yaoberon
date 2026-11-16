package io.github.madzi.yaoberon.lsp;

import io.github.madzi.yaoberon.lsp.service.YaOberonWorkspaceService;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.ServerCapabilities;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageClientAware;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

public class YaOberonLanguageServer implements LanguageServer, LanguageClientAware {

    private final WorkspaceService workspaceService;
    private final TextDocumentService textDocumentService;
    private LanguageClient client;

    public YaOberonLanguageServer() {
        this.workspaceService = new YaOberonWorkspaceService();
        this.textDocumentService = null;
    }

    @Override
    public CompletableFuture<InitializeResult> initialize(final InitializeParams params) {
        ServerCapabilities capabilities = new ServerCapabilities();
        return CompletableFuture.completedFuture(new InitializeResult(capabilities));
    }

    @Override
    public CompletableFuture<Object> shutdown() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void exit() {
        System.exit(0);
    }

    @Override
    public TextDocumentService getTextDocumentService() {
        return textDocumentService;
    }

    @Override
    public WorkspaceService getWorkspaceService() {
        return workspaceService;
    }

    @Override
    public void connect(final LanguageClient client) {
        this.client = client;
    }

    public Optional<LanguageClient> client() {
        return Optional.ofNullable(client);
    }
}