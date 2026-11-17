package io.github.madzi.yaoberon.ir;

public enum IrOpcode {
    LABEL,
    ADD, SUB, MUL, DIV, MOD, NEG,
    AND, OR, XOR, NOT,
    CMP_EQ, CMP_NE, CMP_LT, CMP_LE, CMP_GT, CMP_GE,
    COPY, LOAD, STORE, PARAM,
    JMP, CJMP,
    CALL, ENTER, LEAVE, RET
}
