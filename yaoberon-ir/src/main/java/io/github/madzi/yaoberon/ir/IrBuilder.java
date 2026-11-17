package io.github.madzi.yaoberon.ir;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import io.github.madzi.yaoberon.source.SourceFile;
import io.github.madzi.yaoberon.symbol.RedNode;
import io.github.madzi.yaoberon.symbol.SymbolResolver;

public class IrBuilder {

    private final SourceFile source;
    private final SymbolResolver resolver;
    private final IrModule irModule;

    private IrFunction currentFunction;
    private int nextRegId = 0;
    private int nextLabelId = 0;

    public IrBuilder(final SourceFile source, final SymbolResolver resolver) {
        this.source = source;
        this.resolver = resolver;
        this.irModule = new IrModule(source.uri());
    }

    public IrModule build(final RedNode root) {
        if (null != root) {
            visit(root);
        }
        return irModule;
    }

    private void visit(final RedNode node) {
        if (null == node) {
            return;
        }
        switch (node.kind()) {
            case MODULE_DECLARATION -> visitModule(node);
            case PROCEDURE_DECLARATION -> visitProcedure(node);
            case ASSIGNMENT_STATEMENT -> visitAssignment(node);
            case IF_STATEMENT -> visitIf(node);
            case WHILE_STATEMENT -> visitWhile(node);
            default -> visitChildren(node);
        }
    }

    private void visitModule(final RedNode node) {
        currentFunction = new IrFunction("$init");
        irModule.addFunction(currentFunction);
        currentFunction.emit(IrInstruction.enter());
        visitChildren(node);
        currentFunction.emit(IrInstruction.leave());
        currentFunction.emit(IrInstruction.ret());
    }

    private void visitProcedure(final RedNode node) {
        IrFunction prevFunction = currentFunction;
        nextRegId = 0;
        String procName = extractProcedureName(node);
        currentFunction = new IrFunction(procName);
        irModule.addFunction(currentFunction);
        currentFunction.emit(IrInstruction.enter());
        visitChildren(node);
        currentFunction.emit(IrInstruction.leave());
        currentFunction.emit(IrInstruction.ret());
        currentFunction = prevFunction;
    }

    private void visitAssignment(final RedNode node) {
        if (node instanceof RedNode.Composite composite && composite.children().size() >= 3) {
            RedNode targetNode = composite.children().get(0);
            RedNode exprNode = composite.children().get(2);
            IrOperand valueOp = buildExpression(exprNode);
            String targetName = extractText(targetNode);
            emit(IrInstruction.assign(IrOperand.symbolLabel(targetName), valueOp));
        }
    }

    private void visitIf(final RedNode node) {
        String elseLabel = nextLabel("else");
        String endLabel = nextLabel("if_end");
        if (node instanceof RedNode.Composite composite && composite.children().size() >= 4) {
            RedNode condNode = composite.children().get(1);
            IrOperand condOp = buildExpression(condNode);
            emit(IrInstruction.condJump(condOp, elseLabel));
            visit(composite.children().get(3));
            emit(IrInstruction.jump(endLabel));
            emit(IrInstruction.label(elseLabel));
            if (composite.children().size() > 5) {
                visit(composite.children().get(5));
            }
            emit(IrInstruction.label(endLabel));
        }
    }

    private void visitWhile(final RedNode node) {
        String loopHeader = nextLabel("while_cond");
        String loopEnd = nextLabel("while_end");
        emit(IrInstruction.label(loopHeader));
        if (node instanceof RedNode.Composite composite && composite.children().size() >= 4) {
            RedNode condNode = composite.children().get(1);
            IrOperand condOp = buildExpression(condNode);
            emit(IrInstruction.condJump(condOp, loopEnd));
            visit(composite.children().get(3));
            emit(IrInstruction.jump(loopHeader));
            emit(IrInstruction.label(loopEnd));
        }
    }

    private void visitChildren(final RedNode node) {
        if (node instanceof RedNode.Composite composite) {
            for (RedNode child : composite.children()) {
                visit(child);
            }
        }
    }

    private IrOperand buildExpression(final RedNode exprNode) {
        if (null == exprNode) {
            return IrOperand.constant(0);
        }
        if (SyntaxKind.INT_LITERAL == exprNode.kind()) {
            long val = Long.parseLong(extractText(exprNode));
            return IrOperand.constant(val);
        }
        if (SyntaxKind.IDENTIFIER == exprNode.kind() || SyntaxKind.DESIGNATOR == exprNode.kind()) {
            return IrOperand.symbolLabel(extractText(exprNode));
        }
        if (exprNode instanceof RedNode.Composite composite && composite.children().size() == 3) {
            IrOperand left = buildExpression(composite.children().get(0));
            String opText = extractText(composite.children().get(1));
            IrOperand right = buildExpression(composite.children().get(2));
            IrOpcode opcode = mapOpcode(opText);
            IrOperand.VirtualReg targetReg = nextRegister();
            emit(IrInstruction.binary(opcode, targetReg, left, right));
            return targetReg;
        }
        if (exprNode instanceof RedNode.Composite composite && !composite.children().isEmpty()) {
            return buildExpression(composite.children().get(0));
        }
        return IrOperand.constant(0);
    }

    private IrOpcode mapOpcode(final String op) {
        return switch (op) {
            case "+" -> IrOpcode.ADD;
            case "-" -> IrOpcode.SUB;
            case "*" -> IrOpcode.MUL;
            case "DIV" -> IrOpcode.DIV;
            case "MOD" -> IrOpcode.MOD;
            case "=" -> IrOpcode.CMP_EQ;
            case "#" -> IrOpcode.CMP_NE;
            case "<" -> IrOpcode.CMP_LT;
            case "<=" -> IrOpcode.CMP_LE;
            case ">" -> IrOpcode.CMP_GT;
            case ">=" -> IrOpcode.CMP_GE;
            default -> IrOpcode.ADD;
        };
    }

    private String extractText(final RedNode node) {
        if (node instanceof RedNode.TokenLeaf leaf) {
            return source.piece(leaf.tokenLeaf().token().span()).toString();
        } else if (node instanceof RedNode.Composite composite && !composite.children().isEmpty()) {
            return extractText(composite.children().get(0));
        }
        return "";
    }

    private String extractProcedureName(final RedNode procNode) {
        if (procNode instanceof RedNode.Composite composite) {
            for (RedNode child : composite.children()) {
                if (SyntaxKind.PROCEDURE_HEADING == child.kind()) {
                    if (child instanceof RedNode.Composite headingComposite) {
                        for (RedNode headingChild : headingComposite.children()) {
                            if (SyntaxKind.IDENTIFIER_DEFINITION == headingChild.kind() || SyntaxKind.IDENTIFIER == headingChild.kind()) {
                                return extractText(headingChild);
                            }
                        }
                    }
                }
            }
        }
        return "AnonymousProc";
    }

    private IrOperand.VirtualReg nextRegister() {
        return new IrOperand.VirtualReg(nextRegId++);
    }

    private String nextLabel(final String prefix) {
        return "." + prefix + "_" + (nextLabelId++);
    }

    private void emit(final IrInstruction instr) {
        if (null != currentFunction) {
            currentFunction.emit(instr);
        }
    }
}
