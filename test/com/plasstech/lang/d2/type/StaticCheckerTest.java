package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.plasstech.lang.d2.testing.VarTypeSubject.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.AssignmentNode;
import com.plasstech.lang.d2.parse.node.BinOpNode;
import com.plasstech.lang.d2.parse.node.ConstNode;
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.MainNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.parse.node.UnaryNode;
import com.plasstech.lang.d2.parse.node.VariableNode;
import com.plasstech.lang.d2.parse.node.VariableSetNode;
import com.plasstech.lang.d2.phase.State;

@RunWith(TestParameterInjector.class)
public class StaticCheckerTest {

  @Test
  public void print() {
    checkProgram("print 123");
  }

  @Test
  public void printUnassigned() {
    assertError("print a", "Indeterminable");
    assertError("print (1-3)*a", "Indeterminable");
  }

  @Test
  public void assignInt() {
    State state = safeTypeCheck("a=3");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignUnaryIntConst() {
    State state = safeTypeCheck("a=-3");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignUnaryVar() {
    State state = safeTypeCheck("a=3 b=-a");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(1);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    UnaryNode unaryNode = (UnaryNode) expr;
    assertThat(unaryNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignUnaryExpr() {
    State state = safeTypeCheck("a=3 b=-(a+3)");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(1);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    UnaryNode unaryNode = (UnaryNode) expr;
    assertThat(unaryNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignBool() {
    State state = safeTypeCheck("a=true");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.BOOL);

    Node expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void manyBinOps() {
    SymTab types = checkProgram("a=4 b=5 e=(a>=3) or not (b<3)");

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of e").that(types.lookup("e")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void assignBoolConstantUnaryError() {
    assertError("a=-true", "- operator");
  }

  @Test
  public void assignBoolUnaryError() {
    assertError("a=true b=-a", "- operator");
  }

  @Test
  public void assignIntUnaryOK() {
    SymTab types = checkProgram("a=3 b=!a");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
  }

  @Test
  public void assignDouble() {
    State state = safeTypeCheck("a=3.0");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.DOUBLE);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.DOUBLE);

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.DOUBLE);
  }

  @Test
  public void assignUnaryDoubleConst() {
    State state = safeTypeCheck("a=-3.0");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.DOUBLE);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.DOUBLE);

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.DOUBLE);
    ConstNode<Double> doubleExpr = (ConstNode<Double>) expr;
    assertThat(doubleExpr.value()).isEqualTo(-3.0);
  }

  @Test
  public void assignUnaryVarDouble() {
    State state = safeTypeCheck("a=3.0 b=-a");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.DOUBLE);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.DOUBLE);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(1);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.varType()).isEqualTo(VarType.DOUBLE);

    Node expr = node.expr();
    UnaryNode unaryNode = (UnaryNode) expr;
    assertThat(unaryNode.varType()).isEqualTo(VarType.DOUBLE);
  }

  @Test
  public void assignDoubleExpr() {
    State state = safeTypeCheck("a=3.1+4.4*9.0/3.14 b=4.0 c=a>b");
    SymTab types = state.symbolTable();
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.DOUBLE);
    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.varType()).isEqualTo(VarType.DOUBLE);
  }

  @Test
  public void assignDoubleError() {
    assertError("a=3.0 b=3 b=a", "declared type INT to DOUBLE");
    assertError("a=3 b=3.0 b=a", "declared type DOUBLE to INT");
  }

  @Test
  public void doubleUnaryError() {
    assertError("a=3.0 a=!a", "Cannot apply ! operator to DOUBLE expression");
  }

  @Test
  public void lengthNotStringFailure() {
    assertError("a=length(false)", "Cannot apply LENGTH");
    assertError("a=length(3)", "Cannot apply LENGTH");
  }

  @Test
  public void lengthString() {
    SymTab types = checkProgram("a=length('hi')");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    types = checkProgram("b='hi' a=length(b)");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
  }

  @Test
  public void lengthArray() {
    SymTab types = checkProgram("a=length([1,2,3])");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
  }

  @Test
  public void ascError() {
    assertError("a=asc(false)", "Cannot apply ASC");
    assertError("a=asc(3)", "Cannot apply ASC");
  }

  @Test
  public void asc() {
    SymTab types = checkProgram("a=asc('h')");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    types = checkProgram("b='hello' a=asc(b)");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
  }

  @Test
  public void chrError() {
    assertError("a=chr(false)", "Cannot apply CHR");
    assertError("a=chr('hi')", "Cannot apply CHR");
  }

  @Test
  public void chr() {
    SymTab types = checkProgram("a=chr(65)");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.STRING);
    types = checkProgram("b=66 a=chr(b)");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.STRING);
  }

  @Test
  public void assignBoolConstantUnary() {
    SymTab types = checkProgram("a=not true");

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void assignBoolUnary() {
    SymTab types = checkProgram("a=true b=not a");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void assignExpr() {
    State state = safeTypeCheck("a=3+4-9");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    BinOpNode binOpNode = (BinOpNode) expr;
    assertThat(binOpNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignExprIndeterminable() {
    assertError("a=3+(4-b)", "Indeterminable type");
  }

  @Test
  public void assignExprIndeterminableMultiple() {
    assertError("a=3 b=a+3 c=d", "Indeterminable type");
  }

  @Test
  public void assignMulti() {
    State state = safeTypeCheck("a=3 b=a c = b+4 d=b==c e=3<4 f=d==true print c");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of c").that(types.lookup("c")).isEqualTo(VarType.INT);
    assertWithMessage("type of e").that(types.lookup("e")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of f").that(types.lookup("f")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of d").that(types.lookup("d")).isEqualTo(VarType.BOOL);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(1);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("b");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    VariableNode rhsNode = (VariableNode) expr;
    assertThat(rhsNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignMismatch() {
    assertError("a=true b=3 b=a", "declared type INT to BOOL");
    assertError("a=3 b=true b=a", "declared type BOOL to INT");
  }

  @Test
  public void assignAfterDeclMismatch() {
    assertError("a:int b=true a=b", "declared type INT to BOOL");
    assertError("a:bool b=a b=3", "used before assignment");
    assertError("a=3 a:bool", "already declared as INT");
  }

  @Test
  public void arrayDecl() {
    checkProgram("a:int[3]");
    checkProgram("b=3 a:int[b]");
    checkProgram("b:proc():int {return 0} a:string[b()]");
    checkProgram("a:int[3] b=a");
    checkProgram("a:int[3] b:int[3] b=a");
  }

  @Test
  public void arrayDeclMismatch() {
    assertError("a:int[b]", "Indeterminable size for ARRAY variable 'a'; must be INT");
    assertError("a:int[false]", "must be INT; was BOOL");
    assertError("a:int['hi']", "must be INT; was STRING");
    assertError("a:string['hi']", "must be INT; was STRING");
    assertError("b:proc():string {return ''} a:string[b()]", "must be INT; was STRING");
    // this fails in an unexpected way but at least it fails.
    assertError("b:proc() {} a:string[b]", "Variable 'b' used before assignment");
  }

  @Test
  public void binOpMismatch(@TestParameter({"==", "!="}) String op) {
    assertError(String.format("a=true %s 3", op), "Incompatible types for operator");
    assertError(String.format("a='hi' %s 3", op), "Incompatible types for operator");
  }

  @Test
  public void boolBinOp(@TestParameter({"==", "or", "and", "<", ">", "<=", ">="}) String op) {
    checkProgram(String.format("a=true %s false", op));
  }

  @Test
  public void boolBinOpBad(@TestParameter({"+", "-", "*", "/"}) String op) {
    assertError(String.format("a=true %s false", op), "Cannot apply");
    assertError(String.format("a=true %s 3", op), "Cannot apply");
  }

  @Test
  public void booleanSingleCharMismatch(@TestParameter({"+", "-", "|", "&"}) String c) {
    assertError(String.format("a=true %s 3", c), "Cannot apply");
  }

  @Test
  public void badStringSingleCharMismatch(@TestParameter({"-", "|", "&"}) String c) {
    assertError(String.format("a='hi' %s 3", c), "Cannot apply");
    assertError("a='hi' + 3", "Incompatible types.*STRING.*INT");
  }

  @Test
  public void stringOperators_errors(@TestParameter({"|", "&", "-", "%", "*", "/"}) String c) {
    assertError(String.format("a='hi' %s 'not'", c), "Cannot apply");
  }

  @Test
  public void stringOperators(@TestParameter({"+", "<", ">", "==", "!=", "<=", ">="}) String op) {
    checkProgram(String.format("a='hi' %s 'bye'", op));
  }

  @Test
  public void stringComparator() {
    SymTab symTab = checkProgram("b:bool b='hi' == 'bye'");
    assertThat(symTab.get("b").varType()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void stringAdd() {
    SymTab symTab = checkProgram("b='hi' a='bye' c=a+b");
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.STRING);
    assertThat(symTab.get("b").varType()).isEqualTo(VarType.STRING);
    assertThat(symTab.get("c").varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void stringIndex() {
    SymTab symTab = checkProgram("b='hi' a=b[1]");
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void stringLiteral_index() {
    SymTab symTab = checkProgram("a='hi'[1]");
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void arrayIndexMismatch() {
    assertError(
        "arr=[1,2,3] a=arr['bye']", "Index of ARRAY variable 'arr' must be INT; was STRING");
    assertError("arr=[1,2,3] a=arr[false]", "must be INT; was BOOL");
    assertError("arr=[1,2,3] b='hi' a=arr[b]", "must be INT; was STRING");
  }

  @Test
  public void arrayIndex() {
    SymTab symTab = checkProgram("arr=[1,2,3] a=arr[1]");
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void arrayString() {
    SymTab symTab = checkProgram("arr=['a', 'b', 'c'] a=arr[1 + 1]");
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void arrayLiteral() {
    SymTab symTab = checkProgram("a=[1,2,3]");
    assertThat(symTab.get("a").varType()).isArray();
  }

  @Test
  public void arrayLiteralMismatch() {
    assertError("a=[1,true]", "Inconsistent type");
    assertError("b=3 a=[true,b]", "Inconsistent type");
    assertError("a=[true,b]", "Indeterminable type");
    assertError("a:int[2] a[0]=[1,2]", "declared as ARRAY of INT but.*ARRAY of INT");
  }

  @Test
  public void arrayLiteralGood() {
    SymTab symTab = checkProgram("f:proc():int {return 1} a=[1,f()]");
    VarType varType = symTab.get("a").varType();
    assertThat(varType).isArray();
    assertThat(varType).hasArrayBaseType(VarType.INT);
  }

  @Test
  public void arrayLiteral_index() {
    SymTab symTab = checkProgram("a=[1,2,3][1]"); // NO idea if this will work!
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void arrayOperatorErrors(@TestParameter({"+", "-", "/", "%"}) String c) {
    assertError(String.format("a1 = [1,2,3] %s [2,3,4]", c), " to ARRAY expression");
  }

  @Test
  public void arraySet() {
    SymTab symTab = checkProgram("a:int[1] a[0]=1");
    assertThat(symTab.get("a").varType()).isArray();

    checkProgram("b=3 a:int[1] a[b+1]=1");
  }

  @Test
  public void arrayDeclError() {
    assertError("a:int[0]", "must be positive; was 0");
    assertError("a:int[-1]", "must be positive; was -1");
    assertError("a:int[true]", "must be INT; was BOOL");
  }

  @Test
  public void arraySetTypeError() {
    assertError("a:int[1] a[0]='hi'", "declared as ARRAY of INT but.*STRING");
    assertError("a:bool[1] a[0]=3", "declared as ARRAY of BOOL but.*INT");
    assertError("a:string[1] a[0]=true", "declared as ARRAY of STRING but.*BOOL");
    assertError("a[0]=true", "Unknown variable 'a' used as ARRAY");
    assertError("a=3 a[0]=1", "must be of type ARRAY; was INT");
  }

  @Test
  public void arraySetIndexError() {
    assertError("a:int[1] a['hi']=1", "ARRAY index must be INT; was STRING");
    assertError("a:int[1] a[true]=1", "ARRAY index must be INT; was BOOL");
    assertError("b='hi' a:int[1] a[b]=1", "ARRAY index must be INT; was STRING");
    assertError("b=true a:int[1] a[b]=1", "ARRAY index must be INT; was BOOL");
    assertError("a:int[1] a[-1]=1", "ARRAY index must be non-negative; was -1");
  }

  @Test
  public void arrayGetIndexError() {
    assertError("a:int[1] print a['hi']", "must be INT; was STRING");
    assertError("a:int[1] print a[true]", "must be INT; was BOOL");
    assertError("a:int[1] print a[-1]", "must be non-negative; was -1");
  }

  @Test
  public void stringIndex_error() {
    assertError("b='hi' a=b['bye']", "must be INT; was STRING");
    assertError("b='hi' a=b[false]", "must be INT; was BOOL");
    assertError("b='hi' a='hi'[b]", "must be INT; was STRING");
    assertError("b='hi' a='hi'[-1]", "must be non-negative; was -1");
    assertError("b=3 a=b[3]", "Cannot apply \\[ operator to left operand of type INT");
  }

  @Test
  public void ifElifElse() {
    checkProgram("a=1 if a==1 { print a } elif a == 2 {print 2} else {print 3}");
  }

  @Test
  public void ifStmt() {
    checkProgram("a=true if a { print a }");
  }

  @Test
  public void if_notBool_error() {
    assertError("if 1 { print 2 }", "must be BOOL");
    assertError("a=1 if a { print a }", "must be BOOL");
  }

  @Test
  public void if_notBoolNested_error() {
    assertError("a=1 if a==1 { if (a==1) { if b {print a } } }", "UNKNOWN");
  }

  @Test
  public void if_error() {
    assertError("a=1 if a==1 { a=b }", "Indeterminable");
  }

  @Test
  public void if_duplicated_cases() {
    assertError("a=1 if a==1 { print a } elif a == 1 {print 'a'} ", "Duplicate expression a == 1");
    assertError(
        "a='hi' if a==input { print a } elif a == input {print 'a'} ",
        "Duplicate expression a == INPUT");
    assertError(
        "a=1 if a==1 OR a == 2 { print a } elif a == 1 OR a == 2 {print 'a'} ",
        "Duplicate expression a == 1 OR a == 2");
  }

  @Test
  public void else_error() {
    assertError("a=1 if a==1 {} else {a=b }", "Indeterminable");
  }

  @Test
  public void main_error() {
    assertError("a=1 main { if a==1 {} else {a=b}}", "Indeterminable");
  }

  @Test
  public void main() {
    State state = safeTypeCheck("main {a=3 b=a c=b+4 d=b==c e=3<4 f=d==true}");
    SymTab types = state.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of c").that(types.lookup("c")).isEqualTo(VarType.INT);
    assertWithMessage("type of d").that(types.lookup("d")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of e").that(types.lookup("e")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of f").that(types.lookup("f")).isEqualTo(VarType.BOOL);

    ProgramNode root = state.programNode();
    MainNode mainNode = (MainNode) root.statements().statements().get(0);

    AssignmentNode node = (AssignmentNode) mainNode.block().statements().get(1);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("b");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    VariableNode rhsNode = (VariableNode) expr;
    assertThat(rhsNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void whileLoop() {
    SymTab types = checkProgram("i=0 while i < 30 do b = i == 1 { print i }");

    assertWithMessage("type of i").that(types.lookup("i")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void while_errors() {
    assertError("while a { print a }", "UNKNOWN");
    assertError("while 1 { print 1 }", "INT");
    assertError("while true do i = false + 1 {}", "Cannot apply");
    assertError("while true {i = false + 1}", "Cannot apply");
  }

  @Test
  public void decl_errors() {
    assertError("b=3 a=b a:int", "already declared as INT");
    assertError("a=3 a:bool", "already declared as INT");
    assertError("a:bool a:int", "already declared as BOOL");
    assertError("a:bool a:bool", "already declared as BOOL");
    // This may or may not be an error later
    assertError("a:bool main {a:int}", "already declared as BOOL");
    assertError("a:int b=a", "'a' used before assign");
    assertError("a:int a=true", "declared type INT to BOOL");
    assertError("a:string a=true", "declared type STRING to BOOL");
    assertError("a:int a=''", "declared type INT to STRING");
  }

  @Test
  public void globalDecls_neverUndefined() {
    // Tests bug#39
    checkProgram(
        "      a:string "
            + "p: proc {"
            + "  print a"
            + "}"
            + "setup: proc {"
            + "  a = 'hi'"
            + "}"
            + "setup() "
            + "p()");
  }

  @Test
  public void decl() {
    SymTab types = checkProgram("a:int a=3 b=a");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
  }

  @Test
  public void proc() {
    checkProgram("fib:proc() {a=3} a=true");
    checkProgram("fib:proc(n) : int { n=3 return n}");
    checkProgram("fib:proc(n:int) : int { n=3 return n}");
    checkProgram("fib:proc(n1:int, n2:int) : int { n1=3 n2=n1 return n1}");
    checkProgram("a=true fib:proc() {a:int a=3} ");
    checkProgram(
        "level1:proc() : bool { "
            + " level2:proc() : int  {n=3 return n}"
            + " return false"
            + "} level1()");
    checkProgram("fib:proc(a:int[]) {a[0]=3} fib([1,2,3])");
  }

  @Test
  public void procRecursive() {
    checkProgram(
        "fib:proc(n:int) : int {"
            + "  if n <= 1 {"
            + "    return n"
            + "  } else {"
            + "    return fib(n-1) + fib(n-2)"
            + "  }"
            + "}"
            + "");
  }

  @Test
  public void procIterative() {
    checkProgram(
        "fib2:proc (n:int) : int {"
            + " n1 = 0 "
            + " n2 = 1 "
            + " i=1 while i < n do i = i + 1 { "
            + "  nth = n1 + n2 "
            + "  n1 = n2 "
            + "  n2 = nth "
            + " } "
            + " return nth "
            + "}");
  }

  @Test
  public void procParams_errors() {
    assertError("fib:proc(n1):int {return n1}", "Indeterminable type for RETURN statement");
    assertError("fib:proc(a, b, a) {}", "Duplicate parameter");
    assertError("fib:proc() {a=3 a=true}", "declared type INT to BOOL");
    assertError("a=true fib:proc() {a=3}", "declared type BOOL to INT");
    assertError("fib:proc(n1) {}", "determine type of parameter");
    assertError("fib:proc(n:int) {} fib(true)", "found BOOL, expected INT");
    assertError("fib:proc(a:int[]) {a[0]=3} fib(1)", "expected 1-d ARRAY of INT");
    assertError("fib:proc(a:int) {a=3} fib([1])", "found 1-d ARRAY of INT");
    assertError("fib:proc(n1:rec){} ", "unknown RECORD type rec");
  }

  @Test
  public void proc5Params() {
    checkProgram(
        "       add5:proc(a:int,b:int,c:int,d:int,e:int):int {return a+b+c+d+e}"
            + " println add5(1,2,3,4,5) ");
  }

  @Test
  public void externProc() {
    checkProgram("fib:extern proc()");
    checkProgram("fib:extern proc(n:int) ");
    checkProgram("fib:extern proc(n1:int, n2:int)");
    checkProgram("level1:extern proc(): bool b=level1()");
    checkProgram("fib:extern proc(a:int[]) fib([1,2,3])");
    checkProgram("rec:record {} fib:extern proc(n1:rec)");
  }

  @Test
  public void externProcParams_errors() {
    assertError("fib:extern proc(n1) ", "determine type of parameter");
    assertError("fib:extern proc(n1:rec) ", "unknown RECORD type rec");
    assertError("fib:extern proc(a, b, a)", "Duplicate parameter");
    assertError("fib:extern proc(n:int) fib(true)", "found BOOL, expected INT");
    assertError("fib:extern proc(a:int[]) fib(1)", "expected 1-d ARRAY of INT");
    assertError("fib:extern proc(a:int) fib([1])", "found 1-d ARRAY of INT");
  }

  @Test
  public void return_mismatch() {
    assertError(
        "fib:proc():bool {return 3}",
        "declared to return BOOL but RETURN statement was of type INT");
    assertError("fib:proc(a):int {a='hi' return a}", "INT.*STRING");
    assertError("fib:proc(a:int) {a=3 return a}", "VOID.*INT");

    assertError("fib:proc() {return 3}", "VOID.*INT");
    assertError("fib:proc():int {return}", "INT.*VOID");
  }

  @Test
  public void return_required() {
    assertError("fib:proc():int {}", "Not all codepaths");
    assertError(
        "fib:proc():bool {" + "if false {" + " return false" + "}" + "}", "Not all codepaths");
    assertError(
        "fib:proc():bool {"
            + "if false {"
            + "  if true {"
            + "    return false"
            + "  } elif false {"
            + "    return true"
            + "  } else {"
            + "    print 'hi'"
            + "  }"
            + "}"
            + "}",
        "Not all codepaths");
    assertError("fib:proc():bool {if false {return false} else {print 'hi'}}", "Not all codepaths");
    assertError(
        "fob:proc():int {"
            + "if (false) {"
            + "  if (true) {"
            + "  } elif (3==3) {"
            + "  } else {"
            + "  }"
            + "} elif (3==3) {"
            + "  if (true) {"
            + "    return 3"
            + "  } elif (3==3) {"
            + "    return 3"
            + "  } else {"
            + "    return 3"
            + "  }"
            + "}"
            + "}",
        "Not all codepaths");
  }

  @Test
  public void return_outsideProc() {
    assertError("return 3", "outside a PROC");
  }

  @Test
  public void callErrors() {
    assertError("foo(3)", "PROC 'foo' is undefined");
    assertError("a:int a(3)", "PROC 'a' is undefined");
    assertError("fib:proc(){inner:proc(){}} inner(3)", "PROC 'inner' is undefined");
    // wrong number of params
    assertError(
        "fib:proc(){} fib(3)",
        "Wrong number of arguments in call to PROC 'fib': found 1, expected 0");
    assertError(
        "fib:proc(n:int){} fib(3, 4)",
        "Wrong number of arguments in call to PROC 'fib': found 2, expected 1");
    // indeterminable arg type
    assertError("fib:proc(n) {fib(n)}", "Indeterminable type for parameter 'n' of PROC 'fib'");
    // wrong arg type
    assertError(
        "fib:proc(n:int) {} fib(false)",
        "Incorrect type of parameter 'n' to PROC 'fib': found BOOL, expected INT");
    // can't assign to void
    assertError("fib:proc(n:int) {} x=fib(3)", "Cannot assign value of VOID expression");
  }

  @Test
  public void callInnerProc() {
    checkProgram(
        "      level1: proc(): bool {\n"
            + "  level2: proc(): int {n=3 return n}\n"
            + "  m=level2()\n"
            + "  return m==3\n"
            + "}\n");
  }

  @Test
  public void exit() {
    checkProgram("exit");
    checkProgram("exit 'sorry'");
  }

  @Test
  public void exit_errors() {
    assertError("exit -1", "must be STRING");
    assertError("exit length('sorry')", "must be STRING");
  }

  @Test
  public void input() {
    checkProgram("f=input");
    checkProgram("f=input a=f+' should work'");
  }

  @Test
  public void input_errors() {
    assertError("f:int f=input", "declared type INT to STRING");
    assertError("f=5 f=input", "declared type INT to STRING");
  }

  @Test
  public void recordDefinition_empty() {
    SymTab symTab = checkProgram("r: record{}");
    assertThat(symTab.get("r")).isInstanceOf(RecordSymbol.class);
  }

  @Test
  public void recordDefinition_simple() {
    SymTab symTab = checkProgram("r2: record{s:string i:int b:bool}");
    assertThat(symTab.get("r2")).isInstanceOf(RecordSymbol.class);
  }

  @Test
  public void recordDefinition_recursive() {
    SymTab symTab = checkProgram("rec: record{r:rec}");
    assertThat(symTab.get("rec")).isInstanceOf(RecordSymbol.class);
  }

  @Test
  public void recordDefinition_forward() {
    SymTab symTab = checkProgram("rec1: record{r:rec2} rec2: record{i:int}");
    assertThat(symTab.get("rec1")).isInstanceOf(RecordSymbol.class);
    assertThat(symTab.get("rec2")).isInstanceOf(RecordSymbol.class);
  }

  @Test
  public void recordDefinition_corecursive() {
    SymTab symTab = checkProgram("rec1: record{r:rec2} rec2: record{r:rec1}");
    assertThat(symTab.get("rec1")).isInstanceOf(RecordSymbol.class);
    assertThat(symTab.get("rec2")).isInstanceOf(RecordSymbol.class);
  }

  @Test
  public void recordDefinition_errors() {
    assertError("r: record{f:record{f2:int}}", "nested RECORD 'f' in RECORD 'r'");
    assertError("r: record{p:proc() {} }", "nested PROC 'p' in RECORD 'r'");
    assertError(
        "r: record{i:int f:int f:bool i:int b:bool}",
        "Duplicate field\\(s\\) '\\[f, i\\]' declared in RECORD 'r'");
    assertError("r: record{f:dne}", "unknown RECORD type dne");
    assertError(
        "s=3 r:record{a:string[s]} anr=new r print anr.a",
        "ARRAYs in RECORDs must have constant size");
    assertError(
        "r:record{a:string[1+1]} anr=new r print anr.a",
        "ARRAYs in RECORDs must have constant size");
    assertError(
        "r:record{as:string[1]} anr=new r aa=anr.as x=3 x=aa[0]", "declared type INT to STRING");
  }

  @Test
  public void recordDefinition_duplicate() {
    assertError("r: record{f:int} r:record{b:bool}", "'r' already declared as r: RECORD");
  }

  @Test
  public void recordDefinition_redeclared() {
    assertError("r: int r:record{b:bool}", "redeclared as INT");
  }

  @Test
  public void variableDecl_recordType() {
    SymTab symTab = checkProgram("r2: record{s:string} instance: r2");
    assertThat(symTab.get("r2")).isInstanceOf(RecordSymbol.class);

    Symbol rec = symTab.get("instance");
    assertThat(rec.isAssigned()).isFalse();
    RecordReferenceType refType = (RecordReferenceType) rec.varType();
    assertThat(refType.name()).isEqualTo("r2");
  }

  @Test
  public void variableDecl_errorRecordType() {
    assertError("instance: r1", "Cannot declare variable 'instance' as unknown RECORD type r1");
  }

  @Test
  public void variableDecl_recordLikeTypeButNotQuiteRecordType() {
    assertError(
        "p:proc(){} instance: p", "Cannot declare variable 'instance' as unknown RECORD type p");
  }

  @Test
  public void paramDecl_recordType() {
    SymTab symTab = checkProgram("r1: record{s:string} p:proc(instance:r1){}");
    ProcSymbol proc = (ProcSymbol) symTab.get("p");
    ParamSymbol instance = proc.formal(0);
    RecordReferenceType r1 = (RecordReferenceType) instance.varType();
    assertThat(r1.name()).isEqualTo("r1");
  }

  @Test
  public void paramDecl_errorRecordType() {
    assertError(
        "p:proc(instance:r1){}", "Cannot declare variable 'instance' as unknown RECORD type r1");
  }

  @Test
  public void returnType_recordType() {
    SymTab symTab = checkProgram("r1: record{s:string} p:proc:r1{return null}");
    ProcSymbol proc = (ProcSymbol) symTab.get("p");
    RecordReferenceType r1 = (RecordReferenceType) proc.returnType();
    assertThat(r1.name()).isEqualTo("r1");
  }

  @Test
  public void returnType_errorRecordType() {
    assertError(
        "p:proc:r1 {return null}",
        // WHAT IN THE LIVING HECK? "variable 'return type'"?
        "Cannot declare variable 'return type' as unknown RECORD type r1");
  }

  @Test
  public void assignRecordType() {
    SymTab symTab = checkProgram("r1:record{s:string} var1:r1 var1=null var2:r1 var2=var1");
    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType = (RecordReferenceType) var1.varType();
    assertThat(refType.name()).isEqualTo("r1");

    Symbol var2 = symTab.get("var2");
    RecordReferenceType refType2 = (RecordReferenceType) var2.varType();
    assertThat(refType2.name()).isEqualTo("r1");
  }

  @Test
  public void recordType_compare() {
    SymTab symTab =
        checkProgram("r1:record{s:string} var1=new r1 var2 = new r1 if var1==var2 { print 'same'}");
    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType = (RecordReferenceType) var1.varType();
    assertThat(refType.name()).isEqualTo("r1");

    Symbol var2 = symTab.get("var2");
    RecordReferenceType refType2 = (RecordReferenceType) var2.varType();
    assertThat(refType2.name()).isEqualTo("r1");
  }

  @Test
  public void recordType_compareToNull() {
    SymTab symTab =
        checkProgram("r1:record{s:string} var1=new r1 if var1!=null { print 'not null'}");
    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType = (RecordReferenceType) var1.varType();
    assertThat(refType.name()).isEqualTo("r1");
  }

  @Test
  public void nullEquality() {
    checkProgram("if null != null { print 'not null'}");
    checkProgram("if null == null { print 'not null'}");
    checkProgram("s='' if null == s {print 'not 3'}");
    checkProgram("s='' if s == null {print 'not 3'}");
    checkProgram("a=[3] if null != a {print 'not 3'}");
    checkProgram("rt:record{} r = new rt if r != null {print 'not 3'}");
  }

  @Test
  public void nullErrors() {
    assertError(
        "s='' if null > s { print 'not null'}",
        "Cannot apply > operator to left operand of type NULL");
    assertError(
        "if null < null { print 'not null'}",
        "Cannot apply < operator to left operand of type NULL");
    assertError("if not null {print 'not null'}", "Cannot apply NOT operator to NULL expression");
    assertError("if null == 3 {print 'not 3'}", "Incompatible types for.*NULL.*INT");
  }

  @Test
  public void assignRecordType_inferred() {
    SymTab symTab = checkProgram("r1:record{s:string} var1:r1 var1=null var2=var1");
    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType = (RecordReferenceType) var1.varType();
    assertThat(refType.name()).isEqualTo("r1");

    Symbol var2 = symTab.get("var2");
    RecordReferenceType refType2 = (RecordReferenceType) var2.varType();
    assertThat(refType2.name()).isEqualTo("r1");
  }

  @Test
  public void assignRecordType_mismatch() {
    assertError("r1:record{} var1:r1 var1=null var2:int var2=var1", "to r1: RECORD");
    assertError("r1:record{} r2:record{} var1:r1 var1=null var2:r2 var2=var1", "to r1: RECORD");
    assertError(
        "r1:record{} r2:record{} var1:r1 var1=null var2:r2 var2=null var1=var2", "to r2: RECORD");
    assertError(
        "r1:record{} r2:record{} var1:r1 var1=null var2:r2 var2=null var1=var2", "to r2: RECORD");
    assertError(
        "r1:record{} r2:record{} var1:r1 var1=null var2:r2 var2=null var2=var1", "to r1: RECORD");
  }

  @Test
  public void assignRecordType_procReturnMismatch() {
    assertError(
        "r1:record{i:int} r2:record{} p:proc():r1{return new r2}",
        "but RETURN statement was of type r2: RECORD");
    assertError(
        "r1:record{i:int} r2:record{} p:proc():r2{return new r2} var1:r1 var1=p()",
        "type r1: RECORD to r2: RECORD");
  }

  @Test
  public void newRecord() {
    SymTab symTab = checkProgram("r1:record{s:string} var1=new r1");
    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType = (RecordReferenceType) var1.varType();
    assertThat(refType.name()).isEqualTo("r1");
    assertThat(var1.isAssigned()).isTrue();
  }

  @Test
  public void newRecord_assign() {
    SymTab symTab = checkProgram("r1:record{s:string} var1=new r1 var2=var1");

    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType1 = (RecordReferenceType) var1.varType();
    assertThat(refType1.name()).isEqualTo("r1");
    assertThat(var1.isAssigned()).isTrue();

    Symbol var2 = symTab.get("var2");
    RecordReferenceType refType2 = (RecordReferenceType) var2.varType();
    assertThat(refType2.name()).isEqualTo("r1");
    assertThat(var2.isAssigned()).isTrue();
  }

  @Test
  public void newRecord_procReturnsRecord() {
    SymTab symTab = checkProgram("r1:record{i:int} p:proc():r1{return new r1} var1=p()");

    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType1 = (RecordReferenceType) var1.varType();
    assertThat(refType1.name()).isEqualTo("r1");
    assertThat(var1.isAssigned()).isTrue();
  }

  @Test
  public void newRecord_unknown() {
    assertError("var1=new r2", "unknown RECORD type r2");
    assertError("r1:record{s:string} var1=new r2", "unknown RECORD type r2");
  }

  @Test
  public void newRecord_mismatch() {
    assertError(
        "r1:record{s:string} r2:record{s:string} var1=new r1 var2 = new r2 var2=var1",
        "to r1: RECORD");
    assertError(
        "r1:record{s:string} r2:record{s:string} var1=new r1 var2 = new r2 var1=var2",
        "to r2: RECORD");
    assertError("r1:record{s:string} var1=new r1 var2=1 var1=var2", "to INT");
    assertError("r1:record{s:string} var1=new r1 var2=1 var2=var1", "to r1: RECORD");
  }

  @Test
  public void fieldGet() {
    SymTab symTab = checkProgram("r1:record{s:string i:int} var1=new r1 ii=var1.i");
    Symbol symbol = symTab.get("ii");
    assertThat(symbol.isAssigned()).isTrue();
    assertThat(symbol.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void fieldGet_nested() {
    SymTab symTab = checkProgram("r1:record{i:int} r2:record{rone:r1} var2=new r2 ii=var2.rone.i");
    Symbol symbol = symTab.get("ii");
    assertThat(symbol.isAssigned()).isTrue();
    assertThat(symbol.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void fieldGet_recursive() {
    SymTab symTab = checkProgram("r1:record{i:int next:r1} var1=new r1 var2=var1.next");

    Symbol var2 = symTab.get("var2");
    RecordReferenceType refType2 = (RecordReferenceType) var2.varType();
    assertThat(refType2.name()).isEqualTo("r1");
    assertThat(var2.isAssigned()).isTrue();
  }

  @Test
  public void fieldGet_procReturn() {
    SymTab symTab = checkProgram("r1:record{i:int} p:proc():r1{return new r1} var1=p().i");

    Symbol var1 = symTab.get("var1");
    assertThat(var1.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void fieldGet_mismatch() {
    assertError("r1:record{s:string i:int} var1=new r1 ss:string ss=var1.i", "STRING to INT");
    assertError("r1:record{s:string i:int} var1=new r1 ss='string' ss=var1.i", "STRING to INT");
    assertError("ss='string' ss2=ss.i", "Cannot apply . operator to left operand of type STRING");
  }

  @Test
  public void record_badOp() {
    assertError(
        "r1:record{s:string i:int} var1=new r1 var2=var1+1", "Incompatible types for operator +");
    assertError(
        "r1:record{s:string i:int} var1=new r1 var2=1+var1", "Incompatible types for operator +");
  }

  @Test
  public void fieldSet() {
    SymTab symTab = checkProgram("r1:record{i:int} var1=new r1 var1.i=3 var2=var1.i");
    Symbol symbol = symTab.get("var2");
    assertThat(symbol.isAssigned()).isTrue();
    assertThat(symbol.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void fieldSet_error() {
    assertError("r1:record{i:int} var1=new r1 var1.i=true", "is BOOL");
    assertError("r1:record{i:int s:string} var1=new r1 var1.i=var1.s", "is STRING");
    assertError("r1:record{s:string} var1=new r1 var1.s=0", "is INT");
  }

  @Test
  public void arrayOfRecord() {
    checkProgram(
        "      r:record{a:string} rs:r[2]"
            + "tr = new r "
            + "rs[0] = tr "
            + "tr.a='hi' "
            + "println tr.a");
  }

  @Test
  public void arrayOfRecordError() {
    assertError(
        "      r:record{a:string} " //
            + "ar:r[2] "
            + "ai:int[2] "
            + "ar=ai",
        "ARRAY of r to 1-d ARRAY of INT");
    assertError(
        "      r:record{a:string} " //
            + "ar:r[2] "
            + "ai:int[2] "
            + "ai=ar",
        "ARRAY of INT to 1-d ARRAY of r");
  }

  @Test
  public void advancedRValue() {
    SymTab symtab =
        checkProgram(
            "      r1:record{bar:r2} r2:record{baz:r3[1]} r3:record{qux:string}"
                + " foo:r1[3]"
                + " a=4"
                + " f:proc:int{return 1}"
                + " bam = foo[3+a].bar.baz[f()].qux");
    assertThat(symtab.lookup("bam")).isEqualTo(VarType.STRING);
  }

  @Test
  public void invalidFieldName() {
    assertError(
        "r1:record{field:string} foo=new r1 bam = foo.3",
        "Cannot use expression 3 to get field of RECORD type r1");
    assertError(
        "r1:record{field:string} foo=new r1 bam = foo.'hi'",
        "Cannot use expression 'hi' to get field of RECORD type r1");
    assertError(
        "r1:record{field:string} foo=new r1 bam = foo.'field'",
        "Cannot use expression 'field' to get field of RECORD type r1");
    assertError(
        "r1:record{field:string} foo=new r1 bam = foo.true", "true to get field of RECORD type r1");
    assertError("r1:record{field:string} foo=new r1 f='field' bam = foo.f", "unknown field f");
  }

  @Test
  public void noArgs() {
    SymTab symTab = checkProgram("a=3");
    assertThat(symTab.lookup("ARGS")).isEqualTo(VarType.UNKNOWN);
  }

  @Test
  public void simpleArgs() {
    SymTab symTab = checkProgram("println args[0]");
    assertThat(symTab.lookup("ARGS")).isArray();
    assertThat(symTab.lookup("ARGS")).hasArrayBaseType(VarType.STRING);
  }

  @Test
  public void copyArgs() {
    SymTab symTab = checkProgram("b=args");
    assertThat(symTab.lookup("b")).isArray();
    assertThat(symTab.lookup("b")).hasArrayBaseType(VarType.STRING);
  }

  @Test
  public void complexArgs() {
    SymTab symTab =
        checkProgram(
            "      len=length(args)\r\n"
                + "print 'length is ' println len\r\n"
                + "a=args[0]\r\n"
                + "println 'first is ' + a\r\n");
    assertThat(symTab.lookup("len")).isEqualTo(VarType.INT);
    assertThat(symTab.lookup("a")).isEqualTo(VarType.STRING);
    assertThat(symTab.lookup("ARGS")).isArray();
    assertThat(symTab.lookup("ARGS")).hasArrayBaseType(VarType.STRING);
  }

  @Test
  public void badArgs() {
    assertError("a:bool a=args", "Cannot convert");
    assertError("a:bool a=args[0]", "Cannot convert");
    assertError("a:bool a=length(args)", "Cannot convert");
  }

  @Test
  public void unknownWhile() {
    // Tests bug #204
    assertError("while x < 3 { println x}", "Indeterminable type for expression x");
    assertError("while 3 < x { println x}", "Indeterminable type for expression x");
  }

  @Test
  public void unknownBracket() {
    // Tests bug #205
    assertError("x=a[3]", "Indeterminable type for expression a");
    assertError("p:proc() {x=a[3]}", "Indeterminable type for expression a");
  }

  @Test
  public void badBracket() {
    // Tests bug #205
    assertError("a=0 x=a[3]", "Cannot apply.*operand of type INT");
  }

  private void assertError(String program, String messageShouldContain) {
    State state = unsafeTypeCheck(program);
    assertWithMessage("Should have result error for:\n " + program).that(state.error()).isTrue();
    assertWithMessage("Should have correct error for:\n " + program)
        .that(state.errorMessage())
        .containsMatch(messageShouldContain);
  }

  private SymTab checkProgram(String program) {
    State state = safeTypeCheck(program);
    return state.symbolTable();
  }

  private State safeTypeCheck(String program) {
    State state = unsafeTypeCheck(program);
    if (state.error()) {
      fail(state.errorMessage());
    }
    return state;
  }

  private State unsafeTypeCheck(String program) {
    Lexer lexer = new Lexer(program);
    Parser parser = new Parser(lexer);
    State state = State.create(program).build();
    state = parser.execute(state);
    if (state.error()) {
      fail(
          "Should have passed parse for:\n "
              + program
              + "\nbut failed with error "
              + state.errorMessage());
    }
    StaticChecker checker = new StaticChecker();
    state = checker.execute(state);
    if (state.error()) {
      System.err.println(state.errorMessage());
    }
    return state;
  }
}
