package io.github.madzi.yaoberon.symbol;

import java.util.ArrayList;
import java.util.List;

public class Symbol {

    public enum Kind {
        MODULE, CONSTANT, TYPE, VARIABLE, PROCEDURE, PARAMETER
    }

    private final String name;
    private final Kind kind;
    private final RedNode declarationNode;
    private final List<RedNode> references = new ArrayList<>();
    private OberonType type = OberonType.NONE;

    public Symbol(final String name, final Kind kind, final RedNode declarationNode) {
        this.name = name;
        this.kind = kind;
        this.declarationNode = declarationNode;
    }

    public OberonType type() { return type; }
    public void setType(final OberonType type) { this.type = type; }
    public String name() { return name; }
    public Kind kind() { return kind; }
    public RedNode declarationNode() { return declarationNode; }
    public List<RedNode> references() { return references; }
    public void addReference(final RedNode refNode) { references.add(refNode); }
}
