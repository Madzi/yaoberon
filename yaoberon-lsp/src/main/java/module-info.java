module io.github.madzi.yaoberon.lsp {
    exports io.github.madzi.yaoberon.lsp;

    requires transitive org.eclipse.lsp4j;
    requires io.github.madzi.yaoberon.source;
    requires io.github.madzi.yaoberon.diagnostic;
    requires io.github.madzi.yaoberon.lexer;
    requires io.github.madzi.yaoberon.parser;
    requires io.github.madzi.yaoberon.symbol;
}
