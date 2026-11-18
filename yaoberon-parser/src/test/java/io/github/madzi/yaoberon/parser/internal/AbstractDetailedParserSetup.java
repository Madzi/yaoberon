package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.lexer.Lexer;
import io.github.madzi.yaoberon.parser.ParseContext;
import io.github.madzi.yaoberon.parser.Parser;
import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.source.SourceFile;

class AbstractDetailedParserSetup {

    protected Lexer lexer = Lexer.create();
    protected DetailedParser parser = (DetailedParser) Parser.create();

    protected ParseContext context(final String code) {
        var source = SourceFile.create("mem://test", 0, code);
        var scanResult = lexer.tokenizeAll(source);
        return new ParseContext(source, scanResult.tokens());
    }

    protected String fromNode(final SourceFile source, final GreenNode node) {
        if (node instanceof GreenNode.Composite composite) {
            StringBuilder builder = new StringBuilder();
            for (GreenNode child : composite.children()) {
                builder.append(fromNode(source, child));
            }
            return builder.toString();
        }
        if (node instanceof GreenNode.TokenLeaf leaf) {
            return source.piece(leaf.token().span()).toString();
        }
        return "unknown";
    }
}
