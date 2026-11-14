package io.github.madzi.yaoberon.cli.command;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "lsp", description = "Run Oberon LSP server")
public class LspCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        System.out.println("LSP server starting...");
        return 0;
    }
}
