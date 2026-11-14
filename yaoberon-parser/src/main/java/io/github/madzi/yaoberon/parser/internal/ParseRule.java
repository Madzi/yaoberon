package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.ParseContext;
import io.github.madzi.yaoberon.parser.cst.GreenNode;

@FunctionalInterface
public interface ParseRule {
    GreenNode apply(ParseContext context);
}
