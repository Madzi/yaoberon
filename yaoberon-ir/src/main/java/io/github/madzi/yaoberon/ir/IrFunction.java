package io.github.madzi.yaoberon.ir;

import java.util.ArrayList;
import java.util.List;

public class IrFunction {

    private final String name;
    private final List<IrInstruction> instructions = new ArrayList<>();

    public IrFunction(final String name) {
        this.name = name;
    }

    public String name() { return name; }
    public List<IrInstruction> instructions() { return instructions; }
    public void emit(IrInstruction instr) {
        instructions.add(instr);
    }
}
