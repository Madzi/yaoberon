package io.github.madzi.yaoberon.symbol.internal;

import io.github.madzi.yaoberon.source.SourceFile;
import io.github.madzi.yaoberon.symbol.RedNode;
import io.github.madzi.yaoberon.symbol.SymbolResolver;

public interface DetailedSymbolResolver extends SymbolResolver {

    void resolveTypeDeclaration(RedNode node);

    static DetailedSymbolResolver create(final SourceFile source) {
        return new BaseSymbolResolver(source);
    }
}
