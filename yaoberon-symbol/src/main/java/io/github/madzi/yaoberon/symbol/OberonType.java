package io.github.madzi.yaoberon.symbol;

public sealed interface OberonType {

    enum Kind {
        NONE, INTEGER, REAL, LONGINT, LONGREAL, CHAR, BOOLEAN, SET, ARRAY, RECORD, POINTER
    }

    Kind kind();

    record Primitive(Kind kind) implements OberonType {}
    record Array(OberonType elementType, int length) implements OberonType {
        @Override public Kind kind() { return Kind.ARRAY; }
    }
    record Record(OberonType baseType, String name) implements OberonType {
        @Override public Kind kind() { return Kind.RECORD; }
    }
    record Pointer(OberonType baseType) implements OberonType {
        @Override public Kind kind() { return Kind.POINTER; }
    }
    record None() implements OberonType {
        @Override public Kind kind() { return Kind.NONE; }
    }

    OberonType REAL = new Primitive(Kind.REAL);
    OberonType LONGREAL = new Primitive(Kind.LONGREAL);
    OberonType INTEGER = new Primitive(Kind.INTEGER);
    OberonType LONGINT = new Primitive(Kind.LONGINT);
    OberonType CHAR = new Primitive(Kind.CHAR);
    OberonType BOOLEAN = new Primitive(Kind.BOOLEAN);
    OberonType SET = new Primitive(Kind.SET);
    OberonType NONE = new None();
}
