package io.github.madzi.yaoberon.ir;

import java.util.ArrayList;
import java.util.List;

public record IrModule(String name, List<IrFunction> functions, List<String> globalVariables) {

    public IrModule(final String name) {
        this(name, new ArrayList<>(), new ArrayList<>());
    }

    public String name() {
        return name;
    }

    public void addFunction(final IrFunction function) {
        functions.add(function);
    }

    public void addGlobalVariable(final String varName) {
        globalVariables.add(name);
    }

    public String dump() {
        StringBuilder sb = new StringBuilder();
        sb.append("; IR Module: ").append(name).append("\n\n");
        if (!globalVariables.isEmpty()) {
            sb.append("; Globals:\n");
            for (String globName : globalVariables) {
                sb.append("global ").append(globName).append('\n');
            }
            sb.append('\n');
        }
        for (IrFunction func : functions) {
            sb.append("define ").append(func.name()).append(" {\n");
            for (IrInstruction instr : func.instructions()) {
                sb.append(instr.toString()).append('\n');
            }
            sb.append("}\n\n");
        }
        return sb.toString();
    }
}
