package io.github.madzi.yaoberon.parser;

import io.github.madzi.yaoberon.parser.cst.GreenNode;
import java.util.List;

public record ParseResult(GreenNode root, List<SyntaxError> syntaxErrors) {
}
