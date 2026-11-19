package io.github.madzi.yaoberon.semantic;

import java.util.Optional;

public interface SemanticModel {
    Optional<Symbol> symbol(RedNode node);
    Scope scope(RedNode node);
    Optional<OberonType> type(RedNode node);
}
