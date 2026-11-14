module io.github.madzi.yaoberon.cli {
    requires info.picocli;
    requires io.github.madzi.yaoberon.i18n;
    requires org.jline.terminal;
    requires org.jline.terminal.ffm;
    opens io.github.madzi.yaoberon.cli to info.picocli;
    opens io.github.madzi.yaoberon.cli.command to info.picocli;
}
