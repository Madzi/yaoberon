package io.github.madzi.yaoberon.cli.command;

import picocli.CommandLine.Command;
import java.util.concurrent.Callable;

@Command(name = "format", description = "Format Oberon source")
public class FormatCommand implements Callable<Integer> {

    @Override
    public Integer call() {
        return 0;
    }
}
