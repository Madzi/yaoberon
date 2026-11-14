package io.github.madzi.yaoberon.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "yao", mixinStandardHelpOptions = true, version = "1.0-SNAPSHOT",
         description = "Yet Another Oberon CLI tools")
public class Main implements Runnable {

    @Override
    public void run() {
        System.out.println("Yet Another Oberon CLI");
    }

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
}
