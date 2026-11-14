package io.github.madzi.yaoberon.cli.command;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

import io.github.madzi.yaoberon.cli.Main;
import io.github.madzi.yaoberon.cli.i18n.MessageKeys;
import org.jline.terminal.Terminal;
import org.jline.terminal.TerminalBuilder;

@Command(name = "format", description = "Format Oberon source")
public class FormatCommand implements Callable<Integer> {

    @Override
    public Integer call() throws Exception {
        Terminal terminal = TerminalBuilder.builder().system(true).build();
        terminal.writer().println(Main.I18N.text(MessageKeys.FORMAT_START));
        terminal.flush();
        return 0;
    }
}
