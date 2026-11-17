package io.github.madzi.yaoberon.symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class Scope {

    private final Scope parent;
    private final Map<String, Symbol> symbols = new HashMap<>();

    public Scope() {
        this(null);
    }

    public Scope(Scope parent) {
        this.parent = parent;
    }

    public Scope parent() {
        return parent;
    }

    public Map<String, Symbol> symbols() {
        return symbols;
    }

    public boolean declare(final Symbol symbol) {
        if (symbols.containsKey(symbol.name())) {
            return false;
        }
        symbols.put(symbol.name(), symbol);
        return true;
    }

    public Optional<Symbol> resolve(final String name) {
        Symbol symbol = symbols.get(name);
        if (null != symbol) {
            return Optional.of(symbol);
        }
        if (null != parent) {
            return parent.resolve(name);
        }
        return Optional.empty();
    }
}
