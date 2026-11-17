package io.github.madzi.yaoberon.cli.command;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

import io.github.madzi.yaoberon.lsp.YaOberonLanguageServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;

@Command(name = "lsp", description = "Run Oberon LSP server")
public class LspCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        YaOberonLanguageServer server = new YaOberonLanguageServer();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);
        server.connect(launcher.getRemoteProxy());
        launcher.startListening().get();
        return 0;
    }
}
