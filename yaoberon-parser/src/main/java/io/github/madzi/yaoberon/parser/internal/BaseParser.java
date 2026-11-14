package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.lexer.token.TokenKind;
import io.github.madzi.yaoberon.parser.ParseContext;
import io.github.madzi.yaoberon.parser.ParseResult;
import io.github.madzi.yaoberon.parser.cst.GreenNode;
import io.github.madzi.yaoberon.parser.cst.SyntaxKind;

public class BaseParser implements DetailedParser {

    @Override
    public GreenNode parseIdent(final ParseContext context) {
        if (context.check(TokenKind.IDENT)) {
            return context.consume(SyntaxKind.IDENTIFIER);
        }
        context.reportError("Expected identifier");
        return context.consume(SyntaxKind.ERROR);
    }

    @Override
    public GreenNode parseQualident(final ParseContext context) {
        var parse = new GreenNodeBuilder(context);
        parse.expected(parseIdent(context));
        if (context.check(TokenKind.PERIOD) && context.isNextTokenIdentifier()) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(parseIdent(context));
        }
        return parse.node(SyntaxKind.QUALIDENT);
    }

    @Override
    public GreenNode parseIdentDef(final ParseContext context) {
        var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseIdent);
        parse.optional(TokenKind.TIMES, SyntaxKind.OPERATOR);
        return parse.node(SyntaxKind.IDENTIFIER_DEFINITION);
    }

    @Override
    public GreenNode parseInt(final ParseContext context) {
        return context.consume(SyntaxKind.INT_LITERAL);
    }

    @Override
    public GreenNode parseReal(final ParseContext context) {
        return context.consume(SyntaxKind.REAL_LITERAL);
    }

    @Override
    public GreenNode parseNumber(final ParseContext context) {
        if (context.check(TokenKind.INT)) {
            return parseInt(context);
        }
        if (context.check(TokenKind.REAL)) {
            return parseReal(context);
        }
        context.reportError("Expected number (integer or real)");
        return context.consume(SyntaxKind.ERROR);
    }

    @Override
    public GreenNode parseString(final ParseContext context) {
        return context.consume(SyntaxKind.STRING_LITERAL);
    }

    @Override
    public GreenNode parseConstantDeclaration(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseIdentDef);
        parse.expected(TokenKind.EQL, SyntaxKind.OPERATOR, "Expected '='");
        parse.expected(this::parseConstExpression);
        return parse.node(SyntaxKind.CONSTANT_DECLARATION);
    }

    @Override
    public GreenNode parseConstExpression(final ParseContext context) {
        return parseExpression(context);
    }

    @Override
    public GreenNode parseTypeDeclaration(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseIdentDef);
        parse.expected(TokenKind.EQL, SyntaxKind.OPERATOR, "Expected '='");
        parse.expected(this::parseStrucType);
        return parse.node(SyntaxKind.TYPE_DECLARATION);
    }

    @Override
    public GreenNode parseStrucType(final ParseContext context) {
        if (context.check(TokenKind.ARRAY)) {
            return parseArrayType(context);
        }
        if (context.check(TokenKind.RECORD)) {
            return parseRecordType(context);
        }
        if (context.check(TokenKind.POINTER)) {
            return parsePointerType(context);
        }
        if (context.check(TokenKind.PROCEDURE)) {
            return parseProcedureType(context);
        }
        context.reportError("expected ARRAY, RECORD, POINTER, or PROCEDURE");
        return context.consume(SyntaxKind.ERROR);
    }

    @Override
    public GreenNode parseType(final ParseContext context) {
        if (context.check(TokenKind.IDENT)) {
            return parseQualident(context);
        }
        if (context.inList(TokenKind.ARRAY, TokenKind.RECORD, TokenKind.POINTER, TokenKind.PROCEDURE)) {
            return parseStrucType(context);
        }
        context.reportError("expected ARRAY, RECORD, POINTER, PROCEDURE, or identifier");
        return context.consume(SyntaxKind.ERROR);
    }

    @Override
    public GreenNode parseArrayType(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(TokenKind.ARRAY, SyntaxKind.KEYWORD, "Expected 'ARRAY'");
        parse.expected(this::parseLength);
        while (context.check(TokenKind.COMMA)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseLength);
        }
        parse.expected(TokenKind.OF, SyntaxKind.KEYWORD,"Expected 'OF' in array declaration");
        parse.expected(this::parseType);
        return parse.node(SyntaxKind.ARRAY_TYPE);
    }

    @Override
    public GreenNode parseLength(final ParseContext context) {
        return parseConstExpression(context);
    }

    @Override
    public GreenNode parseRecordType(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(TokenKind.RECORD, SyntaxKind.KEYWORD, "Expected 'RECORD'");
        if (context.check(TokenKind.LPAREN)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseBaseType);
            parse.expected(TokenKind.RPAREN, SyntaxKind.PUNCTUATION, "Expected ')'");
        }
        if (!context.check(TokenKind.END)) {
            parse.expected(this::parseFieldListSeq);
        }
        parse.expected(TokenKind.END, SyntaxKind.KEYWORD, "Expected 'END'");
        return parse.node(SyntaxKind.RECORD_TYPE);
    }

    @Override
    public GreenNode parseBaseType(final ParseContext context) {
        return parseQualident(context);
    }

    @Override
    public GreenNode parseFieldListSeq(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseFieldList);
        while (context.check(TokenKind.SEMICOLON)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseFieldList);
        }
        return parse.node(SyntaxKind.FIELD_LIST_SEQUENCE);
    }

    @Override
    public GreenNode parseFieldList(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseIdentList);
        parse.expected(TokenKind.COLON, SyntaxKind.PUNCTUATION, "Expected ':'");
        parse.expected(this::parseType);
        return parse.node(SyntaxKind.FIELD_LIST);
    }

    @Override
    public GreenNode parseIdentList(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseIdentDef);
        while (context.check(TokenKind.COMMA)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseIdentDef);
        }
        return parse.node(SyntaxKind.IDENTIFIER_LIST);
    }

    @Override
    public GreenNode parsePointerType(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.POINTER);
        parse.keyword(TokenKind.TO);
        parse.expected(this::parseType);
        return parse.node(SyntaxKind.POINTER_TYPE);
    }

    @Override
    public GreenNode parseProcedureType(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.PROCEDURE);
        parse.optional(TokenKind.LPAREN, this::parseFormalParameters);
        return parse.node(SyntaxKind.PROCEDURE_TYPE);
    }

    @Override
    public GreenNode parseVariableDeclartion(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseIdentList);
        parse.punctuation(TokenKind.COLON, ":");
        parse.expected(this::parseType);
        return parse.node(SyntaxKind.VARIABLE_DECLARATION);
    }

    @Override
    public GreenNode parseExpression(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseSimpleExpression);
        if (context.inList(TokenKind.EQL, TokenKind.NEQ, TokenKind.LSS, TokenKind.LEQ, TokenKind.GTR, TokenKind.GEQ, TokenKind.IN, TokenKind.IS)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseSimpleExpression);
        }
        return parse.node(SyntaxKind.EXPRESSION);
    }

    @Override
    public GreenNode parseRelation(final ParseContext context) {
        if (context.inList(TokenKind.EQL, TokenKind.NEQ, TokenKind.LSS, TokenKind.LEQ, TokenKind.GTR, TokenKind.GEQ, TokenKind.IN, TokenKind.IS)) {
            return context.consume(SyntaxKind.OPERATOR);
        }
        context.reportError("Expected relation");
        return context.consume(SyntaxKind.ERROR);
    }

    @Override
    public GreenNode parseSimpleExpression(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        if (context.inList(TokenKind.PLUS, TokenKind.MINUS)) {
            parse.expected(SyntaxKind.OPERATOR);
        }
        parse.expected(this::parseTerm);
        while (context.inList(TokenKind.PLUS, TokenKind.MINUS, TokenKind.OR)) {
            parse.expected(SyntaxKind.OPERATOR);
            parse.expected(this::parseTerm);
        }
        return parse.node(SyntaxKind.SIMPLE_EXPRESSION);
    }

    @Override
    public GreenNode parseAddOp(final ParseContext context) {
        if (context.inList(TokenKind.PLUS, TokenKind.MINUS, TokenKind.OR)) {
            return context.consume(SyntaxKind.OPERATOR);
        }
        context.reportError("Expected '+', '-', or 'OR'");
        return context.consume(SyntaxKind.ERROR);
    }

    @Override
    public GreenNode parseTerm(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseFactor);
        while (context.inList(TokenKind.TIMES, TokenKind.RDIV, TokenKind.DIV, TokenKind.MOD, TokenKind.AND)) {
            parse.expected(SyntaxKind.OPERATOR);
            parse.expected(this::parseFactor);
        }
        return parse.node(SyntaxKind.TERM);
    }

    @Override
    public GreenNode parseMulOperator(final ParseContext context) {
        if (context.inList(TokenKind.TIMES, TokenKind.RDIV, TokenKind.DIV, TokenKind.MOD, TokenKind.AND)) {
            return context.consume(SyntaxKind.OPERATOR);
        }
        context.reportError("Expected '*', '/', 'DIV', 'MOD', or '&'");
        return context.consume(SyntaxKind.ERROR);
    }

    @Override
    public GreenNode parseFactor(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        if (context.check(TokenKind.INT)) {
            return parseInt(context);
        }
        if (context.check(TokenKind.REAL)) {
            return parseReal(context);
        }
        if (context.check(TokenKind.STRING)) {
            return parseString(context);
        }
        if (context.check(TokenKind.NIL)) {
            return context.consume(SyntaxKind.NIL);
        }
        if (context.check(TokenKind.TRUE)) {
            return context.consume(SyntaxKind.TRUE);
        }
        if (context.check(TokenKind.FALSE)) {
            return context.consume(SyntaxKind.FALSE);
        }
        if (context.check(TokenKind.LBRACE)) {
            return parseSet(context);
        }
        if (context.check(TokenKind.IDENT)) {
            parse.expected(this::parseDesignator);
            parse.optional(TokenKind.LPAREN, this::parseActualParameters);
            while (context.inList(TokenKind.PERIOD, TokenKind.LBRAK, TokenKind.ARROW)) {
                parse.expected(this::parseSelector);
            }
            return parse.node(SyntaxKind.FACTOR);
        }
        if (context.check(TokenKind.LPAREN)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseExpression);
            parse.punctuation(TokenKind.RPAREN, ")");
            return parse.node(SyntaxKind.FACTOR);
        }
        if (context.check(TokenKind.NOT)) {
            parse.expected(SyntaxKind.OPERATOR);
            parse.expected(this::parseFactor);
            return parse.node(SyntaxKind.FACTOR);
        }
        context.reportError("Expected term");
        return context.consume(SyntaxKind.ERROR);
    }

    @Override
    public GreenNode parseDesignator(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseQualident);
        while (context.inList(TokenKind.PERIOD, TokenKind.LBRAK, TokenKind.ARROW)) {
            parse.expected(this::parseSelector);
        }
        return parse.node(SyntaxKind.DESIGNATOR);
    }

    @Override
    public GreenNode parseSelector(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        if (context.check(TokenKind.PERIOD)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseIdent);
        } else if (context.check(TokenKind.LBRAK)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseExpressionList);
            parse.punctuation(TokenKind.RBRAK, "]");
        } else if (context.check(TokenKind.ARROW)) {
            parse.expected(SyntaxKind.OPERATOR);
        }
        return parse.node(SyntaxKind.SELECTOR);
    }

    @Override
    public GreenNode parseSet(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.punctuation(TokenKind.LBRACE, "{");
        if (!context.check(TokenKind.RBRACE)) {
            parse.expected(this::parseElement);
            while (context.check(TokenKind.COMMA)) {
                parse.expected(SyntaxKind.PUNCTUATION);
                parse.expected(this::parseElement);
            }
        }
        parse.punctuation(TokenKind.RBRACE, "}");
        return parse.node(SyntaxKind.SET);
    }

    @Override
    public GreenNode parseElement(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseExpression);
        if (context.check(TokenKind.UPTO)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseExpression);
        }
        return parse.node(SyntaxKind.ELEMENT);
    }

    @Override
    public GreenNode parseExpressionList(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseExpression);
        while (context.check(TokenKind.COMMA)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseExpression);
        }
        return parse.node(SyntaxKind.EXPRESSION_LIST);
    }

    @Override
    public GreenNode parseActualParameters(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.punctuation(TokenKind.LPAREN, "(");
        if (!context.check(TokenKind.RPAREN)) {
            parse.expected(this::parseExpressionList);
        }
        parse.punctuation(TokenKind.RPAREN, ")");
        return parse.node(SyntaxKind.ACTUAL_PARAMETERS);
    }

    @Override
    public GreenNode parseStatement(final ParseContext context) {
        if (context.check(TokenKind.IF)) {
            return parseIfStatement(context);
        }
        if (context.check(TokenKind.CASE)) {
            return parseCaseStatement(context);
        }
        if (context.check(TokenKind.WHILE)) {
            return parseWhileStatement(context);
        }
        if (context.check(TokenKind.REPEAT)) {
            return parseRepeatStatement(context);
        }
        if (context.check(TokenKind.FOR)) {
            return parseForStatement(context);
        }
        if (context.check(TokenKind.IDENT)) {
            final var parse = new GreenNodeBuilder(context);
            parse.expected(this::parseDesignator);
            parse.optional(TokenKind.LPAREN, this::parseActualParameters);
            while (context.inList(TokenKind.PERIOD, TokenKind.LBRAK, TokenKind.ARROW)) {
                parse.expected(this::parseSelector);
            }
            if (context.check(TokenKind.BECOMES)) {
                parse.expected(TokenKind.BECOMES, SyntaxKind.OPERATOR, "Expected ':='");
                parse.expected(this::parseExpression);
                return parse.node(SyntaxKind.ASSIGNMENT_STATEMENT);
            }
            parse.optional(TokenKind.LPAREN, this::parseActualParameters);
            return parse.node(SyntaxKind.PROCEDURE_CALL_STATEMENT);
        }
        return context.consume(SyntaxKind.EMPTY);
    }

    @Override
    public GreenNode parseAssignment(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseDesignator);
        parse.expected(TokenKind.BECOMES, SyntaxKind.OPERATOR, "Expected ':='");
        parse.expected(this::parseExpression);
        return parse.node(SyntaxKind.ASSIGNMENT_STATEMENT);
    }

    @Override
    public GreenNode parseProcedureCall(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseDesignator);
        parse.optional(TokenKind.LPAREN, this::parseActualParameters);
        return parse.node(SyntaxKind.PROCEDURE_CALL_STATEMENT);
    }

    @Override
    public GreenNode parseStatementSequence(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        if (!context.check(TokenKind.SEMICOLON)) {
            parse.expected(this::parseStatement);
        }
        while (context.check(TokenKind.SEMICOLON)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            if (context.inList(TokenKind.END, TokenKind.ELSIF, TokenKind.ELSE, TokenKind.UNTIL, TokenKind.RETURN)) {
                break;
            }
            if (!context.check(TokenKind.SEMICOLON)) {
                parse.expected(this::parseStatement);
            }
        }
        return parse.node(SyntaxKind.STATEMENT_SEQUENCE);
    }

    @Override
    public GreenNode parseIfStatement(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.IF);
        parse.expected(this::parseExpression);
        parse.keyword(TokenKind.THEN);
        parse.expected(this::parseStatementSequence);
        while (context.check(TokenKind.ELSIF)) {
            parse.expected(SyntaxKind.KEYWORD);
            parse.expected(this::parseExpression);
            parse.keyword(TokenKind.THEN);
            parse.expected(this::parseStatementSequence);
        }
        if (context.check(TokenKind.ELSE)) {
            parse.expected(SyntaxKind.KEYWORD);
            parse.expected(this::parseStatementSequence);
        }
        parse.keyword(TokenKind.END);
        return parse.node(SyntaxKind.IF_STATEMENT);
    }

    @Override
    public GreenNode parseCaseStatement(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.CASE);
        parse.expected(this::parseExpression);
        parse.keyword(TokenKind.OF);
        parse.expected(this::parseCase);
        while (context.check(TokenKind.BAR)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseCase);
        }
        parse.keyword(TokenKind.END);
        return parse.node(SyntaxKind.CASE_STATEMENT);
    }

    @Override
    public GreenNode parseCase(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseCaseLabelList);
        parse.expected(TokenKind.COLON, SyntaxKind.PUNCTUATION, "Expected ':'");
        parse.expected(this::parseStatementSequence);
        return parse.node(SyntaxKind.CASE);
    }

    @Override
    public GreenNode parseCaseLabelList(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseLabelRange);
        while (context.check(TokenKind.COMMA)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseLabelRange);
        }
        return parse.node(SyntaxKind.CASE_LABEL_LIST);
    }

    @Override
    public GreenNode parseLabelRange(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseLabel);
        parse.optional(TokenKind.UPTO, this::parseLabel);
        return parse.node(SyntaxKind.LABEL_RANGE);
    }

    @Override
    public GreenNode parseLabel(final ParseContext context) {
        if (context.check(TokenKind.INT)) {
            return parseInt(context);
        }
        if (context.check(TokenKind.STRING)) {
            return parseString(context);
        }
        if (context.check(TokenKind.IDENT)) {
            return parseIdent(context);
        }
        context.reportError("Expected integer, string, or identifier");
        return context.consume(SyntaxKind.ERROR);
    }

    @Override
    public GreenNode parseWhileStatement(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.WHILE);
        parse.expected(this::parseExpression);
        parse.keyword(TokenKind.DO);
        parse.expected(this::parseStatementSequence);
        while (context.check(TokenKind.ELSIF)) {
            parse.keyword(TokenKind.ELSIF);
            parse.expected(this::parseExpression);
            parse.keyword(TokenKind.DO);
            parse.expected(this::parseStatementSequence);
        }
        parse.keyword(TokenKind.END);
        return parse.node(SyntaxKind.WHILE_STATEMENT);
    }

    @Override
    public GreenNode parseRepeatStatement(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.REPEAT);
        parse.expected(this::parseStatementSequence);
        parse.keyword(TokenKind.UNTIL);
        parse.expected(this::parseExpression);
        return parse.node(SyntaxKind.REPEAT_STATEMENT);
    }

    @Override
    public GreenNode parseForStatement(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.FOR);
        parse.expected(this::parseIdent);
        parse.expected(TokenKind.BECOMES, SyntaxKind.OPERATOR, "Expected ':='");
        parse.expected(this::parseExpression);
        parse.expected(TokenKind.TO, SyntaxKind.KEYWORD, "Expected 'TO' in FOR statement");
        parse.expected(this::parseExpression);
        parse.optional(TokenKind.BY, this::parseConstExpression);
        parse.keyword(TokenKind.DO);
        parse.expected(this::parseStatementSequence);
        parse.keyword(TokenKind.END);
        return parse.node(SyntaxKind.FOR_STATEMENT);
    }

    @Override
    public GreenNode parseProcedureDeclaration(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseProcedureHeading);
        parse.punctuation(TokenKind.SEMICOLON, ";");
        parse.expected(this::parseProcedureBody);
        parse.expected(this::parseIdent);
        return parse.node(SyntaxKind.PROCEDURE_DECLARATION);
    }

    @Override
    public GreenNode parseProcedureHeading(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.PROCEDURE);
        parse.expected(this::parseIdentDef);
        parse.optional(TokenKind.LPAREN, this::parseFormalParameters);
        return parse.node(SyntaxKind.PROCEDURE_HEADING);
    }

    @Override
    public GreenNode parseProcedureBody(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseDeclarationSequence);
        if (context.check(TokenKind.BEGIN)) {
            parse.expected(SyntaxKind.KEYWORD);
            if (!context.check(TokenKind.RETURN)) {
                parse.expected(this::parseStatementSequence);
            }
        }
        if (context.check(TokenKind.RETURN)) {
            parse.expected(SyntaxKind.KEYWORD);
            parse.expected(this::parseExpression);
        }
        parse.keyword(TokenKind.END);
        return parse.node(SyntaxKind.PROCEDURE_BODY);
    }

    @Override
    public GreenNode parseDeclarationSequence(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.optional(TokenKind.CONST, this::parseConstSection);
        parse.optional(TokenKind.TYPE, this::parseTypeSection);
        parse.optional(TokenKind.VAR, this::parseVarSection);
        while (context.check(TokenKind.PROCEDURE)) {
            parse.expected(this::parseProcedureDeclaration);
            parse.punctuation(TokenKind.SEMICOLON, ";");
        }
        return parse.node(SyntaxKind.DECLARATION_SEQUENCE);
    }

    @Override
    public GreenNode parseConstSection(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.CONST);
        while (context.check(TokenKind.IDENT)) {
            parse.expected(this::parseConstantDeclaration);
            parse.punctuation(TokenKind.SEMICOLON, ";");
        }
        return parse.node(SyntaxKind.CONST_SECTION);
    }

    @Override
    public GreenNode parseTypeSection(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.TYPE);
        while (context.check(TokenKind.IDENT)) {
            parse.expected(this::parseTypeDeclaration);
            parse.punctuation(TokenKind.SEMICOLON, ";");
        }
        return parse.node(SyntaxKind.TYPE_SECTION);
    }

    @Override
    public GreenNode parseVarSection(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.keyword(TokenKind.VAR);
        while (context.check(TokenKind.IDENT)) {
            parse.expected(this::parseVariableDeclartion);
            parse.punctuation(TokenKind.SEMICOLON, ";");
        }
        return parse.node(SyntaxKind.VAR_SECTION);
    }

    @Override
    public GreenNode parseFormalParameters(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(TokenKind.LPAREN, SyntaxKind.PUNCTUATION, "Expected '('");
        if (!context.check(TokenKind.RPAREN)) {
            parse.expected(this::parseFPSection);
            while (context.check(TokenKind.SEMICOLON)) {
                parse.expected(SyntaxKind.PUNCTUATION);
                parse.expected(this::parseFPSection);
            }
        }
        parse.expected(TokenKind.RPAREN, SyntaxKind.PUNCTUATION, "Expected ')");
        if (context.check(TokenKind.COLON)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseQualident);
        }
        return parse.node(SyntaxKind.FORMAL_PARAMETERS);
    }

    @Override
    public GreenNode parseFPSection(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        if (context.inList(TokenKind.CONST, TokenKind.VAR)) {
            parse.expected(SyntaxKind.KEYWORD);
        }
        parse.expected(this::parseIdent);
        while (context.check(TokenKind.COMMA)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseIdent);
        }
        parse.expected(TokenKind.COLON, SyntaxKind.PUNCTUATION, "Expected ':'");
        parse.expected(this::parseFormalType);
        return parse.node(SyntaxKind.FP_SECTION);
    }

    @Override
    public GreenNode parseFormalType(ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        if (context.check(TokenKind.ARRAY)) {
            parse.expected(SyntaxKind.KEYWORD);
            parse.expected(TokenKind.OF, SyntaxKind.KEYWORD, "Expected 'OF' after 'ARRAY'");
        }
        parse.expected(this::parseQualident);
        return parse.node(SyntaxKind.FORMAL_TYPE);
    }

    @Override
    public GreenNode parseModule(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(TokenKind.MODULE, SyntaxKind.KEYWORD, "Expected 'MODULE'");
        parse.expected(this::parseIdent);
        parse.expected(TokenKind.SEMICOLON, SyntaxKind.PUNCTUATION, "Expected ';'");
        if (context.check(TokenKind.IMPORT)) {
            parse.expected(this::parseImportList);
        }
        parse.expected(this::parseDeclarationSequence);
        if (context.check(TokenKind.BEGIN)) {
            parse.expected(SyntaxKind.KEYWORD);
            parse.expected(this::parseStatementSequence);
        }
        parse.expected(TokenKind.END, SyntaxKind.KEYWORD, "Expected 'END'");
        parse.expected(this::parseIdent);
        parse.expected(TokenKind.PERIOD, SyntaxKind.PUNCTUATION, "Expected '.'");
        return parse.node(SyntaxKind.MODULE_DECLARATION);
    }

    @Override
    public GreenNode parseImportList(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(TokenKind.IMPORT, SyntaxKind.KEYWORD, "Expected 'IMPORT'");
        parse.expected(this::parseImport);
        while (context.check(TokenKind.COMMA)) {
            parse.expected(SyntaxKind.PUNCTUATION);
            parse.expected(this::parseImport);
        }
        parse.expected(TokenKind.SEMICOLON, SyntaxKind.PUNCTUATION, "Expected ';'");
        return parse.node(SyntaxKind.IMPORT_SECTION);
    }

    @Override
    public GreenNode parseImport(final ParseContext context) {
        final var parse = new GreenNodeBuilder(context);
        parse.expected(this::parseIdent);
        if (context.check(TokenKind.BECOMES)) {
            parse.expected(SyntaxKind.OPERATOR);
            parse.expected(this::parseIdent);
        }
        return parse.node(SyntaxKind.IMPORT_DECLARATION);
    }

    @Override
    public ParseResult parse(final ParseContext context) {
        GreenNode root = parseModule(context);
        return new ParseResult(root, context.errors());
    }
}
