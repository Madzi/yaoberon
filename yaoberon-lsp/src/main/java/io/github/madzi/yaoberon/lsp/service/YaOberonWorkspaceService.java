package io.github.madzi.yaoberon.lsp.service;

import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.services.WorkspaceService;

public class YaOberonWorkspaceService implements WorkspaceService {

    @Override
    public void didChangeConfiguration(final DidChangeConfigurationParams params) {
    }

    @Override
    public void didChangeWatchedFiles(final DidChangeWatchedFilesParams params) {
    }
}
