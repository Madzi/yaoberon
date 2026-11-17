package io.github.madzi.yaoberon.symbol;

import io.github.madzi.yaoberon.source.SourceFile;
import io.github.madzi.yaoberon.symbol.internal.BaseSymbolResolver;
import java.util.Optional;

public interface SymbolResolver {

    Scope findScopeAt(RedNode node);

    void resolve(RedNode root);

    Optional<Symbol> findSymbolFor(RedNode node);

    static SymbolResolver create(final SourceFile source) {
        return new BaseSymbolResolver(source);
    }
}
