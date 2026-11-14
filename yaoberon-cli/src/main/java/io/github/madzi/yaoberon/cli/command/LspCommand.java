package io.github.madzi.yaoberon.cli.command;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

import io.github.madzi.yaoberon.cli.Main;
import io.github.madzi.yaoberon.cli.i18n.MessageKeys;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

@Command(name = "lsp", description = "Run Oberon LSP server")
public class LspCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        terminal.writer().println(Main.I18N.text(MessageKeys.LSP_START));
        terminal.flush();
        return 0;
    }
}
