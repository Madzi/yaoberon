package io.github.madzi.yaoberon.symbol.internal;

import io.github.madzi.yaoberon.parser.cst.SyntaxKind;
import io.github.madzi.yaoberon.source.SourceFile;
import io.github.madzi.yaoberon.symbol.OberonType;
import io.github.madzi.yaoberon.symbol.RedNode;
import io.github.madzi.yaoberon.symbol.Scope;
import io.github.madzi.yaoberon.symbol.Symbol;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public class BaseSymbolResolver implements DetailedSymbolResolver {

    private final Map<RedNode, Symbol> nodeToSymbol = new HashMap<>();
    private final Map<RedNode, Scope> nodeToScope = new HashMap<>();
    private final SourceFile source;
    private Scope currentScope;

    public BaseSymbolResolver(final SourceFile source) {
        this.source = source;
        this.currentScope = new Scope();
        initBuiltinTypes(currentScope);
    }

    @Override
    public Scope findScopeAt(final RedNode node) {
        RedNode curr = node;
        while (null != curr) {
            Scope scope = nodeToScope.get(curr);
            if (null != scope) {
                return scope;
            }
            curr = curr.parent();
        }
        return currentScope;
    }

    @Override
    public void resolve(final RedNode root) {
        if (null == root) {
            return;
        }
        visit(root);
    }

    @Override
    public Optional<Symbol> findSymbolFor(final RedNode node) {
        if (null == node) {
            return Optional.empty();
        }
        Symbol direct = nodeToSymbol.get(node);
        if (null != direct) {
            return Optional.of(direct);
        }
        RedNode curr = node;
        while (null != curr) {
            Symbol sym = nodeToSymbol.get(curr);
            if (null != sym) {
                return Optional.of(sym);
            }
            curr = curr.parent();
        }
        return Optional.empty();
    }

    private Map<SyntaxKind, Consumer<RedNode>> processors = Map.of(
            SyntaxKind.IMPORT_DECLARATION, this::processImportDeclaration,
            SyntaxKind.CONSTANT_DECLARATION, this::processConstantDeclaration,
            SyntaxKind.TYPE_DECLARATION, this::processTypeDeclaration,
            SyntaxKind.VARIABLE_DECLARATION, this::processVariableDeclaration,
            SyntaxKind.PROCEDURE_HEADING, this::processProcedureHeading,
            SyntaxKind.QUALIDENT, this::processIdentifierUse,
            SyntaxKind.DESIGNATOR, this::processIdentifierUse
    );

    private void visit(final RedNode node) {
        if (null == node) {
            return;
        }
        nodeToScope.put(node, currentScope);
        SyntaxKind kind = node.kind();
        if (SyntaxKind.MODULE_DECLARATION == kind) {
            currentScope = new Scope(currentScope);
            visitChildren(node);
            currentScope = currentScope.parent();
            return;
        }
        if (SyntaxKind.PROCEDURE_DECLARATION == kind) {
            processProcedureDeclaration(node);
            return;
        }
        var processor = processors.get(kind);
        if (null != processor) {
            processor.accept(node);
        }
        visitChildren(node);
    }

    private void visitChildren(final RedNode node) {
        if (node instanceof RedNode.Composite composite) {
            for (RedNode child : composite.children()) {
                visit(child);
            }
        }
    }

    @Override
    public void resolveTypeDeclaration(final RedNode node) {
        if (node instanceof RedNode.Composite composite) {
            for (RedNode child : composite.children()) {
                registerIdentifier(child, Symbol.Kind.TYPE);
                break;
            }
        }
    }

    private void processTypeDeclaration(final RedNode node) {
        if (node instanceof RedNode.Composite composite) {
            for (RedNode child : composite.children()) {
                registerIdentifier(child, Symbol.Kind.TYPE);
                break;
            }
        }
    }

    private void processProcedureHeading(final RedNode node) {
        if (!(node instanceof RedNode.Composite composite)) {
            return;
        }
        for (RedNode child : composite.children()) {
            registerIdentifier(child, Symbol.Kind.PROCEDURE);
            break;
        }
        for (RedNode child : composite.children()) {
            if (SyntaxKind.FORMAL_PARAMETERS == child.kind()) {
                processFormalParameters(child);
            }
        }
    }

    private void processImportDeclaration(final RedNode node) {
        if (node instanceof RedNode.Composite composite) {
            for (RedNode child : composite.children()) {
                registerIdentifier(child, Symbol.Kind.MODULE);
            }
        }
    }

    private void processProcedureDeclaration(final RedNode node) {
        RedNode headingNode = findFirstChildOfKind(node, SyntaxKind.PROCEDURE_HEADING);
        if (null == headingNode) {
            return;
        }
        registerIdentifierInParentScope(headingNode);
        currentScope = new Scope(currentScope);
        if (null != headingNode) {
            RedNode formalParams = findFirstChildOfKind(headingNode, SyntaxKind.FORMAL_PARAMETERS);
            if (null != formalParams) {
                processFormalParameters(formalParams);
            }
        }
        visitChildren(node);
        currentScope = currentScope.parent();
    }

    private void processFormalParameters(final RedNode node) {
        if (node instanceof RedNode.Composite composite) {
            for (RedNode child : composite.children()) {
                if (SyntaxKind.FP_SECTION == child.kind()) {
                    collectIdentList(findFirstChildOfKind(child, SyntaxKind.IDENTIFIER_LIST), Symbol.Kind.PARAMETER, OberonType.NONE);
                }
            }
        }
    }

    private void processVariableDeclaration(final RedNode node) {
        if (node instanceof RedNode.Composite composite) {
            RedNode identList = findFirstChildOfKind(composite, SyntaxKind.IDENTIFIER_LIST);
            if (null == identList) {
                return;
            }
            RedNode typeNode = extractTypeNode(composite);
            Optional<OberonType> varType = resolveVarType(typeNode);
            if (varType.isPresent()) {
                String typeName = extractIdentifierText(typeNode);
                if (!typeName.isEmpty()) {
                    currentScope.resolve(typeName).ifPresent(typeSymbol -> {
                        typeSymbol.addReference(typeNode);
                        nodeToSymbol.put(typeNode, typeSymbol);
                    });
                }
            }
        }
    }

    private void collectIdentList(RedNode node, Symbol.Kind kind, OberonType type) {
        if (node instanceof RedNode.Composite composite) {
            for (RedNode child : composite.children()) {
                registerIdentifier(child, kind, type);
            }
        }
    }

    private void registerIdentifier(final RedNode child, final Symbol.Kind kind) {
        registerIdentifier(child, kind, OberonType.NONE);
    }

    private void registerIdentifier(final RedNode node, final Symbol.Kind kind, final OberonType type) {
        if (!(node instanceof RedNode.Composite composite)) {
            return;
        }
        for (RedNode child : composite.children()) {
            if (SyntaxKind.IDENTIFIER_DEFINITION == child.kind() || SyntaxKind.IDENTIFIER == child.kind()) {
                String name = extractIdentifierText(child);
                boolean exported = child.kind() == SyntaxKind.IDENTIFIER_DEFINITION; // FIXME
                if (!name.isEmpty()) {
                    Symbol symbol = new Symbol(name, kind, child, exported);
                    symbol.setType(type);
                    currentScope.declare(symbol);
                    nodeToSymbol.put(child, symbol);
                }
                break;
            }
        }
    }

    private void registerIdentifierInParentScope(final RedNode node) {
        if (!(node instanceof RedNode.Composite composite)) {
            return;
        }
        for (RedNode child : composite.children()) {
            if (SyntaxKind.IDENTIFIER_DEFINITION == child.kind() || SyntaxKind.IDENTIFIER == child.kind()) {
                String name = extractIdentifierText(child);
                boolean exported = child.kind() == SyntaxKind.IDENTIFIER_DEFINITION; // FIXME
                if (!name.isEmpty()) {
                    Symbol procSymbol = new Symbol(name, Symbol.Kind.PROCEDURE, child, exported);
                    currentScope.declare(procSymbol);
                    nodeToSymbol.put(child, procSymbol);
                }
                break;
            }
        }
    }

    private RedNode extractTypeNode(RedNode composite) {
        if (!(composite instanceof RedNode.Composite comp)) {
            return null;
        }
        var children = comp.children();
        return children.size() >= 3 ? children.get(children.size() - 1) : null;
    }

    private Optional<OberonType> resolveVarType(final RedNode typeNode) {
        if (null == typeNode) {
            return Optional.empty();
        }
        String typeName = extractIdentifierText(typeNode);
        var resolved = currentScope.resolve(typeName);
        if (resolved.isPresent()) {
            var typeSymbol = resolved.get();
            if (Symbol.Kind.TYPE == typeSymbol.kind()) {
                return Optional.of(typeSymbol.type());
            } else {
                return Optional.of(OberonType.NONE);
            }
        }
        return Optional.of(OberonType.NONE);
    }

    private void processConstantDeclaration(final RedNode constDeclNode) {
        if (constDeclNode instanceof RedNode.Composite composite) {
            for (RedNode child : composite.children()) {
                registerIdentifier(child, Symbol.Kind.CONSTANT);
                break;
            }
        }
    }

    private OberonType resolveType(final RedNode typeNode) {
        if (null == typeNode) {
            return OberonType.NONE;
        }
        String typeName = extractIdentifierText(typeNode);
        return switch (typeName) {
            case "SET" -> OberonType.SET;
            case "BOOLEAN" -> OberonType.BOOLEAN;
            case "CHAR" -> OberonType.CHAR;
            case "INTEGER" -> OberonType.INTEGER;
            case "LONGINT" -> OberonType.LONGINT;
            case "REAL" -> OberonType.REAL;
            case "LONGREAL" -> OberonType.LONGREAL;
            default -> {
                var resolved = currentScope.resolve(typeName);
                if (resolved.isPresent() && resolved.get().kind() == Symbol.Kind.TYPE) {
                    yield resolved.get().type();
                }
                yield new OberonType.Record(null, typeName, Map.of()); // FIXME
            }
        };
    }

    // private void processDesignator(final RedNode node) {
    //     if (!(node instanceof RedNode.Composite composite)) {
    //         return; // FIXME
    //     }
    //     var children = composite.children();
    //     RedNode head = children.getFirst();
    //     var found = currentScope.resolve(extractIdentifierText(head));
    //     if (found.isEmpty()) {
    //         return; // FIXME
    //     }
    //     Symbol sym = found.get();
    //     nodeToSymbol.put(head, sym);
    //     if (Symbol.Kind.MODULE == sym.kind() && children.size() > 1 && children.get(1).kind() == SyntaxKind.SELECTOR) {
    //         RedNode member = selectorIdent(children.get(1)); // FIXME
    //     }
    //
    // }

    private void processIdentifierUse(final RedNode useNode) {
        if (nodeToSymbol.containsKey(useNode)) {
            return;
        }
        if (useNode.kind() == SyntaxKind.QUALIDENT && useNode instanceof RedNode.Composite composite) {
            var children = composite.children();
            if (children.size() >= 3) {
                RedNode moduleNode = children.get(0);
                RedNode memberNode = children.get(2);
                String moduleName = extractIdentifierText(moduleNode);
                String memberName = extractIdentifierText(memberNode);
                if (!moduleName.isEmpty()) {
                    makeReference(moduleName, moduleNode);
                }
                if (!memberName.isEmpty()) {
                    makeReference(memberName, memberNode);
                }
                return;
            }
        }
        String name = extractIdentifierText(useNode);
        if (!name.isEmpty()) {
            makeReference(name, useNode);
        }
    }

    private void makeReference(final String name, final RedNode useNode) {
        currentScope.resolve(name).ifPresent(symbol -> {
                symbol.addReference(useNode);
                nodeToSymbol.put(useNode, symbol);
        });
    }

    private RedNode findFirstChildOfKind(final RedNode parent, final SyntaxKind targetKind) {
        if (parent instanceof RedNode.Composite composite) {
            for (RedNode child : composite.children()) {
                if (child.kind() == targetKind) {
                    return child;
                }
                RedNode found = findFirstChildOfKind(child, targetKind);
                if (null != found) {
                    return found;
                }
            }
        }
        return null;
    }

    private String extractIdentifierText(final RedNode node) {
        return switch (node) {
            case RedNode.TokenLeaf leaf -> source.piece(leaf.tokenLeaf().token().span()).toString();
            case RedNode.Composite composite -> {
                for (RedNode child : composite .children()) {
                    String text = extractIdentifierText(child);
                    if (!text.isEmpty()) {
                        yield text;
                    }
                }
                yield "";
            }
            default -> "";
        };
    }

    private void initBuiltinTypes(final Scope scope) {
        declareBuiltinType(scope, "INTEGER", OberonType.INTEGER);
        declareBuiltinType(scope, "REAL", OberonType.REAL);
        declareBuiltinType(scope, "LONGINT", OberonType.LONGINT);
        declareBuiltinType(scope, "CHAR", OberonType.CHAR);
        declareBuiltinType(scope, "LONGREAL", OberonType.LONGREAL);
        declareBuiltinType(scope, "BOOLEAN", OberonType.BOOLEAN);
        declareBuiltinType(scope, "SET", OberonType.SET);
    }

    private void declareBuiltinType(final Scope scope, final String name, final OberonType type) {
        Symbol symbol = new Symbol(name, Symbol.Kind.TYPE, null, false);
        symbol.setType(type);
        scope.declare(symbol);
    }
}
