module io.github.madzi.yaoberon.cli {
    requires info.picocli;
    opens io.github.madzi.yaoberon.cli to info.picocli;
    opens io.github.madzi.yaoberon.cli.command to info.picocli;
}
