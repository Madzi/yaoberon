package io.github.madzi.yaoberon.ir;

public sealed interface IrOperand {
    record VirtualReg(int id) implements IrOperand {
        @Override public String toString() { return "$t" + id; }
    }
    record Constant(long value) implements IrOperand {
        @Override public String toString() { return String.valueOf(value); }
    }
    record SymbolLabel(String name) implements IrOperand {
        @Override public String toString() { return name; }
    }

    static IrOperand virtualReg(final int id) {
        return new VirtualReg(id);
    }

    static IrOperand constant(final long value) {
        return new Constant(value);
    }

    static IrOperand symbolLabel(final String name) {
        return new SymbolLabel(name);
    }
}
