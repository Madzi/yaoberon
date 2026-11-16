package io.github.madzi.yaoberon.cli.command;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

import io.github.madzi.yaoberon.cli.Main;
import io.github.madzi.yaoberon.cli.i18n.MessageKeys;
import io.github.madzi.yaoberon.lsp.YaOberonLanguageServer;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

@Command(name = "lsp", description = "Run Oberon LSP server")
public class LspCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        terminal.writer().println(Main.I18N.text(MessageKeys.LSP_START));
        terminal.flush();
        YaOberonLanguageServer server = new YaOberonLanguageServer();
        Launcher<LanguageClient> launcher = LSPLauncher.createServerLauncher(server, System.in, System.out);
        server.connect(launcher.getRemoteProxy());
        launcher.startListening().get();
        return 0;
    }
}
