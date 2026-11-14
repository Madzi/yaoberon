package io.github.madzi.yaoberon.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

import java.util.ResourceBundle;

import io.github.madzi.yaoberon.cli.command.FormatCommand;
import io.github.madzi.yaoberon.cli.command.LspCommand;
import io.github.madzi.yaoberon.cli.i18n.MessageKeys;
import io.github.madzi.yaoberon.i18n.MessageService;

@Command(name = "yao", mixinStandardHelpOptions = true, version = "1.0-SNAPSHOT",
         description = "Yet Another Oberon CLI tools",
         subcommands = {
             LspCommand.class, FormatCommand.class
         })
public class Main implements Runnable {

    public static final MessageService I18N = MessageService.create(ResourceBundle.getBundle("i18n.messages"));

    @Override
    public void run() {
        System.out.println(I18N.text(MessageKeys.CLI_WELCOME));
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
