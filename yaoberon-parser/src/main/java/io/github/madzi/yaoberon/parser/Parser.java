package io.github.madzi.yaoberon.parser;

import io.github.madzi.yaoberon.parser.internal.BaseParser;

public interface Parser {

    ParseResult parse(ParseContext context);

    static Parser create() {
        return new BaseParser();
    }
}
