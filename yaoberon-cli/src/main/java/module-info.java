module io.github.madzi.yaoberon.cli {
    requires io.github.madzi.yaoberon.i18n;
    requires io.github.madzi.yaoberon.source;
    requires io.github.madzi.yaoberon.lexer;
    requires io.github.madzi.yaoberon.parser;
    requires info.picocli;
    requires org.jline.terminal;
    requires org.jline.terminal.ffm;
    opens io.github.madzi.yaoberon.cli to info.picocli;
    opens io.github.madzi.yaoberon.cli.command to info.picocli;
}
