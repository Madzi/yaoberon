package io.github.madzi.yaoberon.symbol;

import java.util.Optional;

public interface SemanticModel {
    Optional<Symbol> symbol(RedNode node);
    Scope scope(RedNode node);
    Optional<OberonType> type(RedNode node);
}
