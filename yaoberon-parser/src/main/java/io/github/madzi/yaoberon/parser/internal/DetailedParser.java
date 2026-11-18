package io.github.madzi.yaoberon.parser.internal;

import io.github.madzi.yaoberon.parser.ParseContext;
import io.github.madzi.yaoberon.parser.Parser;
import io.github.madzi.yaoberon.parser.cst.GreenNode;

public interface DetailedParser extends Parser {

    // digit  =   "0" | "1" | "2" | "3" | "4" | "5" | "6" | "7" | "8" | "9".
    // hexDigit  =  digit | "A" | "B" | "C" | "D" | "E" | "F".
    // ident  =  letter {letter | digit}.
    GreenNode parseIdent(ParseContext context);
    // qualident  =  [ident "."] ident.
    GreenNode parseQualident(ParseContext context);
    // identdef = ident ["*"].
    GreenNode parseIdentDef(ParseContext context);
    // integer  =  digit {digit} | digit {hexDigit} "H".
    GreenNode parseInt(ParseContext context);
    // real  =  digit {digit} "." {digit} [ScaleFactor].
    // ScaleFactor  =  ("E" |"D") ["+" | "-"] digit {digit}.
    GreenNode parseReal(ParseContext context);
    // number  =  integer | real.
    GreenNode parseNumber(ParseContext context);
    // string  =  "'" {character} "'" | digit {hexdigit} "X".
    GreenNode parseString(ParseContext context);
    // ConstDeclaration  =  identdef "=" ConstExpression.
    GreenNode parseConstantDeclaration(ParseContext context);
    // ConstExpression  =  expression.
    GreenNode parseConstExpression(ParseContext context);
    // TypeDeclaration  =  identdef "=" StrucType.
    GreenNode parseTypeDeclaration(ParseContext context);
    // StrucType  =  ArrayType | RecordType | PointerType | ProcedureType.
    GreenNode parseStrucType(ParseContext context);
    // type  =  qualident | StrucType.
    GreenNode parseType(ParseContext context);
    // ArrayType  =  "ARRAY" length {"," length} "OF" type.
    GreenNode parseArrayType(ParseContext context);
    // length  =  ConstExpression.
    GreenNode parseLength(ParseContext context);
    // RecordType  =  "RECORD" ["(" BaseType ")"] [FieldListSequence] "END".
    GreenNode parseRecordType(ParseContext context);
    // BaseType  =  qualident.
    GreenNode parseBaseType(ParseContext context);
    // FieldListSequence  =  FieldList {";" FieldList}.
    GreenNode parseFieldListSeq(ParseContext context);
    // FieldList  =  IdentList ":" type.
    GreenNode parseFieldList(ParseContext context);
    // IdentList  =  identdef {"," identdef}.
    GreenNode parseIdentList(ParseContext context);
    // PointerType  =  "POINTER" "TO" type.
    GreenNode parsePointerType(ParseContext context);
    // ProcedureType  =  "PROCEDURE" [FormalParameters].
    GreenNode parseProcedureType(ParseContext context);
    // VariableDeclaration  =  IdentList ":" type.
    GreenNode parseVariableDeclaration(ParseContext context);
    // expression  =  SimpleExpression [relation SimpleExpression].
    GreenNode parseExpression(ParseContext context);
    // relation  =  "=" | "#" | "<" | "<=" | ">" | ">=" | "IN" | "IS".
    GreenNode parseRelation(ParseContext context);
    // SimpleExpression  =  ["+" | "-"] term {AddOperator term}.
    GreenNode parseSimpleExpression(ParseContext context);
    // AddOperator  =  "+" | "-" | "OR".
    GreenNode parseAddOp(ParseContext context);
    // term  =  factor {MulOperator factor}.
    GreenNode parseTerm(ParseContext context);
    // MulOperator  =  "*" | "/" | "DIV" | "MOD" | "&".
    GreenNode parseMulOperator(ParseContext context);
    // factor  =  number | string | "NIL" | "TRUE" | "FALSE" | set | designator [ActualParameters] | "(" expression ")" | "~" factor.
    GreenNode parseFactor(ParseContext context);
    // designator  =  qualident {selector}.
    GreenNode parseDesignator(ParseContext context);
    // selector  =  "." ident | "[" ExpList "]" | "^" |  "(" qualident ")".
    GreenNode parseSelector(ParseContext context);
    // set  =  "{" [element {"," element}] "}".
    GreenNode parseSet(ParseContext context);
    // element  =  expression [".." expression].
    GreenNode parseElement(ParseContext context);
    // ExpList  =  expression {"," expression}.
    GreenNode parseExpressionList(ParseContext context);
    // ActualParameters  =  "(" [ExpList] ")" .
    GreenNode parseActualParameters(ParseContext context);
    // statement  =  [assignment | ProcedureCall | IfStatement | CaseStatement | WhileStatement | RepeatStatement | ForStatement].
    GreenNode parseStatement(ParseContext context);
    // assignment  =  designator ":=" expression.
    GreenNode parseAssignment(ParseContext context);
    // ProcedureCall  =  designator [ActualParameters].
    GreenNode parseProcedureCall(ParseContext context);
    // StatementSequence  =  statement {";" statement}.
    GreenNode parseStatementSequence(ParseContext context);
    // IfStatement  =  "IF" expression "THEN" StatementSequence {"ELSIF" expression "THEN" StatementSequence} ["ELSE" StatementSequence] "END".
    GreenNode parseIfStatement(ParseContext context);
    // CaseStatement  =  "CASE" expression "OF" case {"|" case} "END".
    GreenNode parseCaseStatement(ParseContext context);
    // Case  =  CaseLabelList ":"  StatementSequence.
    GreenNode parseCase(ParseContext context);
    // CaseLabelList  = LabelRange {"," LabelRange}.
    GreenNode parseCaseLabelList(ParseContext context);
    // LabelRange  =  label [".." label].
    GreenNode parseLabelRange(ParseContext context);
    // label  =  integer | string | ident.
    GreenNode parseLabel(ParseContext context);
    // WhileStatement  =  "WHILE" expression "DO" StatementSequence {"ELSIF" expression "DO" StatementSequence} "END".
    GreenNode parseWhileStatement(ParseContext context);
    // RepeatStatement  =  "REPEAT" StatementSequence "UNTIL" expression.
    GreenNode parseRepeatStatement(ParseContext context);
    // ForStatement  =  "FOR" ident ":=" expression "TO" expression ["BY" ConstExpression] "DO" StatementSequence "END".
    GreenNode parseForStatement(ParseContext context);
    // ProcedureDeclaration  =  ProcedureHeading ";" ProcedureBody ident.
    GreenNode parseProcedureDeclaration(ParseContext context);
    // ProcedureHeading  =  "PROCEDURE" identdef [FormalParameters].
    GreenNode parseProcedureHeading(ParseContext context);
    // ProcedureBody  =  DeclarationSequence ["BEGIN" StatementSequence] ["RETURN" expression] "END".
    GreenNode parseProcedureBody(ParseContext context);
    // DeclarationSequence  =  ["CONST" {ConstDeclaration ";"}] ["TYPE" {TypeDeclaration ";"}] ["VAR" {VariableDeclaration ";"}] {ProcedureDeclaration ";"}.
    GreenNode parseDeclarationSequence(ParseContext context);

    GreenNode parseConstSection(ParseContext context);

    GreenNode parseTypeSection(ParseContext context);

    GreenNode parseVarSection(ParseContext context);
    // FormalParameters  =  "(" [FPSection {";" FPSection}] ")" [":" qualident].
    GreenNode parseFormalParameters(ParseContext context);
    // FPSection  =  ["CONST" | "VAR"] ident {"," ident} ":" FormalType.
    GreenNode parseFPSection(ParseContext context);
    // FormalType  =  ["ARRAY" "OF"] qualident.
    GreenNode parseFormalType(ParseContext context);
    // module  =  "MODULE" ident ";" [ImportList] DeclarationSequence ["BEGIN" StatementSequence] "END" ident "." .
    GreenNode parseModule(ParseContext context);
    // ImportList  =  "IMPORT" import {"," import} ";".
    GreenNode parseImportList(ParseContext context);
    // import  =  ident [":=" ident].
    GreenNode parseImport(ParseContext context);
}
