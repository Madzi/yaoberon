package io.github.madzi.yaoberon.ir;

public record IrInstruction(
        IrOpcode opcode,
        IrOperand result,
        IrOperand op1,
        IrOperand op2
) {

    private static final String PREFIX = "  ";

    public static IrInstruction enter() {
        return new IrInstruction(IrOpcode.ENTER, null, null, null);
    }

    public static IrInstruction leave() {
        return new IrInstruction(IrOpcode.LEAVE, null, null, null);
    }

    public static IrInstruction ret() {
        return new IrInstruction(IrOpcode.RET, null, null, null);
    }

    public static IrInstruction label(final String name) {
        return new IrInstruction(IrOpcode.LABEL, IrOperand.symbolLabel(name), null, null);
    }

    public static IrInstruction binary(final IrOpcode op, final IrOperand res, final IrOperand op1, final IrOperand op2) {
        return new IrInstruction(op, res, op1, op2);
    }

    public static IrInstruction assign(final IrOperand res, final IrOperand value) {
        return new IrInstruction(IrOpcode.COPY, res, value, null);
    }

    public static IrInstruction condJump(final IrOperand reg, String labelName) {
        return new IrInstruction(IrOpcode.CJMP, IrOperand.symbolLabel(labelName), reg, null);
    }

    public static IrInstruction jump(final String label) {
        return new IrInstruction(IrOpcode.JMP, IrOperand.symbolLabel(label), null, null);
    }

    @Override
    public String toString() {
        if (IrOpcode.LABEL == opcode) {
            return result + ":";
        }
        if (null == result && null == op1 && null == op2) {
            return PREFIX + opcode;
        }
        if (null == result && null != op1 && null == op2) {
            return PREFIX + "%s %s".formatted(opcode, op1);
        }
        if (null == op2) {
            return PREFIX + "%s = %s %s".formatted(result, opcode, op1);
        }
        return PREFIX + "%s = %s %s, %s".formatted(result, opcode, op1, op2);
    }
}
