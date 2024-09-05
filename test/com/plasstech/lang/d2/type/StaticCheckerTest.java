package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static com.plasstech.lang.d2.testing.VarTypeSubject.assertThat;
import static com.plasstech.lang.d2.type.testing.StaticCheckerSubject.assertThatTypeChecking;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.parse.node.AssignmentNode;
import com.plasstech.lang.d2.parse.node.BinOpNode;
import com.plasstech.lang.d2.parse.node.ConstNode;
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.IncDecNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.parse.node.StatementNode;
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
    assertThatTypeChecking("print a").hasError("Indeterminable");
    assertThatTypeChecking("print (1-3)*a").hasError("Indeterminable");
  }

  @Test
  public void printVoid() {
    assertThatTypeChecking("f:proc{} print f()").hasError("Cannot print VOID");
  }

  @Test
  public void assignInt() {
    State state = assertThatTypeChecking("a=3").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignLong() {
    State state = assertThatTypeChecking("a=3L").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.LONG);

    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.LONG);

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.LONG);
  }

  @Test
  public void assignUnaryIntConst() {
    State state = assertThatTypeChecking("a=-3").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);

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
    State state = assertThatTypeChecking("a=3 b=-a").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookupRecursive("b")).isEqualTo(VarType.INT);

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
    State state = assertThatTypeChecking("a=3 b=-(a+3)").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookupRecursive("b")).isEqualTo(VarType.INT);

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
    State state = assertThatTypeChecking("a=true").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.BOOL);

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
    SymbolTable types = checkProgram("a=4 b=5L e=(a>=3) or not (b<3L)");

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookupRecursive("b")).isEqualTo(VarType.LONG);
    assertWithMessage("type of e").that(types.lookupRecursive("e")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void assignBoolConstantUnaryError() {
    assertThatTypeChecking("a=-true").hasError("- operator");
  }

  @Test
  public void assignBoolUnaryError() {
    assertThatTypeChecking("a=true b=-a").hasError("- operator");
  }

  @Test
  public void assignIntUnaryOK() {
    SymbolTable types = checkProgram("a=3 b=!a");
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookupRecursive("b")).isEqualTo(VarType.INT);
  }

  @Test
  public void assignDouble() {
    State state = assertThatTypeChecking("a=3.0").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.DOUBLE);

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
    State state = assertThatTypeChecking("a=-3.0").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.DOUBLE);

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
    State state = assertThatTypeChecking("a=3.0 b=-a").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.DOUBLE);
    assertWithMessage("type of b").that(types.lookupRecursive("b")).isEqualTo(VarType.DOUBLE);

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
    State state = assertThatTypeChecking("a=3.1+4.4*9.0/3.14 b=4.0 c=a>b").succeeds();
    SymbolTable types = firstSymTab(state);
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.DOUBLE);
    ProgramNode root = state.programNode();
    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.lvalue();
    assertThat(var.varType()).isEqualTo(VarType.DOUBLE);
  }

  @Test
  public void assignDoubleError() {
    assertThatTypeChecking("a=3.0 b=3 b=a").hasError("declared type INT to DOUBLE");
    assertThatTypeChecking("a=3 b=3.0 b=a").hasError("declared type DOUBLE to INT");
  }

  @Test
  public void doubleUnaryError() {
    assertThatTypeChecking("a=3.0 a=!a").hasError("Cannot apply ! operator to DOUBLE expression");
  }

  @Test
  public void lengthNotStringFailure() {
    assertThatTypeChecking("a=length(false)").hasError("Cannot apply LENGTH");
    assertThatTypeChecking("a=length(3)").hasError("Cannot apply LENGTH");
  }

  @Test
  public void lengthString() {
    SymbolTable types = checkProgram("a=length('hi')");
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
    types = checkProgram("b='hi' a=length(b)");
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
  }

  @Test
  public void lengthArray() {
    SymbolTable types = checkProgram("a=length([1,2,3])");
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
  }

  @Test
  public void ascError() {
    assertThatTypeChecking("a=asc(false)").hasError("Cannot apply ASC");
    assertThatTypeChecking("a=asc(3)").hasError("Cannot apply ASC");
  }

  @Test
  public void asc() {
    SymbolTable types = checkProgram("a=asc('h')");
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
    types = checkProgram("b='hello' a=asc(b)");
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
  }

  @Test
  public void chrError() {
    assertThatTypeChecking("a=chr(false)").hasError("Cannot apply CHR");
    assertThatTypeChecking("a=chr('hi')").hasError("Cannot apply CHR");
  }

  @Test
  public void chr() {
    SymbolTable types = checkProgram("a=chr(65)");
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.STRING);
    types = checkProgram("b=66 a=chr(b)");
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.STRING);
  }

  @Test
  public void assignBoolConstantUnary() {
    SymbolTable types = checkProgram("a=not true");

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void assignBoolUnary() {
    SymbolTable types = checkProgram("a=true b=not a");
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of b").that(types.lookupRecursive("b")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void assignExpr() {
    State state = assertThatTypeChecking("a=3+4-9").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);

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
    assertThatTypeChecking("a=3+(4-b)").hasError("Indeterminable type");
  }

  @Test
  public void assignExprIndeterminableMultiple() {
    assertThatTypeChecking("a=3 b=a+3 c=d").hasError("Indeterminable type");
  }

  @Test
  public void assignMulti() {
    State state =
        assertThatTypeChecking("a=3 b=a c = b+4 d=b==c e=3<4 f=d==true print c").succeeds();
    SymbolTable types = firstSymTab(state);

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookupRecursive("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of c").that(types.lookupRecursive("c")).isEqualTo(VarType.INT);
    assertWithMessage("type of e").that(types.lookupRecursive("e")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of f").that(types.lookupRecursive("f")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of d").that(types.lookupRecursive("d")).isEqualTo(VarType.BOOL);

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
    assertThatTypeChecking("a=true b=3 b=a").hasError("declared type INT to BOOL");
    assertThatTypeChecking("a=3 b=true b=a").hasError("declared type BOOL to INT");
  }

  @Test
  public void assignAfterDeclMismatch() {
    assertThatTypeChecking("a:int b=true a=b").hasError("declared type INT to BOOL");
    assertThatTypeChecking("a=3 a:bool").hasError("already declared as INT");
  }

  @Test
  public void usedAfterNotAssigned() {
    assertThatTypeChecking("a:bool b=a").hasError("used before assignment");
    assertThatTypeChecking("f:proc {a:bool b=a}").hasError("used before assignment");
  }

  @Test
  public void declArray() {
    checkProgram("a:int[3]");
    checkProgram("b=3 a:int[b]");
    checkProgram("b:proc():int {return 0} a:string[b()]");
    checkProgram("a:int[3] b=a");
    checkProgram("a:int[3] b:int[3] b=a");
  }

  @Test
  public void declEmptyArray() {
    // These are both allowed
    checkProgram("a:int[0]");
    checkProgram("b=0 a:int[b]");
  }

  @Test
  public void arrayDeclMismatch() {
    assertThatTypeChecking("a:int[b]")
        .hasError("Indeterminable size for ARRAY variable 'a'; must be INT");
    assertThatTypeChecking("a:int[false]").hasError("must be INT; was BOOL");
    assertThatTypeChecking("a:int['hi']").hasError("must be INT; was STRING");
    assertThatTypeChecking("a:string['hi']").hasError("must be INT; was STRING");
    assertThatTypeChecking("b:proc():string {return ''} a:string[b()]")
        .hasError("must be INT; was STRING");
    // this fails in an unexpected way but at least it fails.
    assertThatTypeChecking("b:proc() {} a:string[b]")
        .hasError("Variable 'b' used before assignment");
  }

  @Test
  public void binOpMismatch(@TestParameter({"==", "!="}) String op) {
    assertThatTypeChecking(String.format("a=true %s 3", op))
        .hasError("Incompatible types for operator");
    assertThatTypeChecking(String.format("a='hi' %s 3", op))
        .hasError("Incompatible types for operator");
  }

  @Test
  public void boolBinOp(@TestParameter({"==", "or", "and", "<", ">", "<=", ">="}) String op) {
    checkProgram(String.format("a=true %s false", op));
  }

  @Test
  public void boolBinOpBad(@TestParameter({"+", "-", "*", "/"}) String op) {
    assertThatTypeChecking(String.format("a=true %s false", op)).hasError("Cannot apply");
    assertThatTypeChecking(String.format("a=true %s 3", op)).hasError("Cannot apply");
  }

  @Test
  public void intBinOpBad(@TestParameter({"+", "-", "*", "/"}) String op) {
    assertThatTypeChecking(String.format("a=3 %s 0y03", op))
        .hasError("Incompatible types for operator " + op);
    assertThatTypeChecking(String.format("a=3L %s 3", op))
        .hasError("Incompatible types for operator " + op);
  }

  @Test
  public void booleanSingleCharMismatch(@TestParameter({"+", "-", "|", "&"}) String c) {
    assertThatTypeChecking(String.format("a=true %s 3", c)).hasError("Cannot apply");
  }

  @Test
  public void badStringSingleCharMismatch(@TestParameter({"-", "|", "&"}) String c) {
    assertThatTypeChecking(String.format("a='hi' %s 3", c)).hasError("Cannot apply");
    assertThatTypeChecking("a='hi' + 3").hasError("Incompatible types.*STRING.*INT");
  }

  @Test
  public void stringOperators_errors(@TestParameter({"|", "&", "-", "%", "*", "/"}) String c) {
    assertThatTypeChecking(String.format("a='hi' %s 'not'", c)).hasError("Cannot apply");
  }

  @Test
  public void stringOperators(@TestParameter({"+", "<", ">", "==", "!=", "<=", ">="}) String op) {
    checkProgram(String.format("a='hi' %s 'bye'", op));
  }

  @Test
  public void stringComparator() {
    SymbolTable symTab = checkProgram("b:bool b='hi' == 'bye'");
    assertThat(symTab.get("b").varType()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void stringAdd() {
    SymbolTable symTab = checkProgram("b='hi' a='bye' c=a+b");
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.STRING);
    assertThat(symTab.get("b").varType()).isEqualTo(VarType.STRING);
    assertThat(symTab.get("c").varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void stringAddToNull_error() {
    assertThatTypeChecking("b='hi' a=b+null").hasError("Cannot add NULL to STRING");
    assertThatTypeChecking("a='hi'+null").hasError("Cannot add NULL to STRING");
    assertThatTypeChecking("b='hi' a=null+b")
        .hasError("Cannot apply \\+ operator to left operand of type NULL");
  }

  @Test
  public void stringIndex() {
    SymbolTable symTab = checkProgram("b='hi' a=b[1]");
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void range() {
    checkProgram("r=2:4 b=r");
    SymbolTable symbolTable = checkProgram("a=2 b=4 r=a:b s=r");
    assertThat(symbolTable.get("r").varType()).isEqualTo(VarType.RANGE);
    assertThat(symbolTable.get("s").varType()).isEqualTo(VarType.RANGE);
  }

  @Test
  public void rangeIndex() {
    SymbolTable symbolTable = checkProgram("r=2:4 a=r[0]");
    assertThat(symbolTable.get("a").varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void rangeBadIndex() {
    assertThatTypeChecking("r=2:4 a=r[false]")
        .hasError("Index of RANGE variable 'r' must be INT; was BOOL");
    assertThatTypeChecking("r=2:4 a=r[2]").hasError("index must be 0 or 1; was 2");
  }

  @Test
  public void badConstantRange() {
    assertThatTypeChecking("r=1.0:4").hasError("Cannot apply : operator to.*DOUBLE");
    assertThatTypeChecking("r=1:4.0").hasError("Incompatible types.*INT but right is DOUBLE");
    assertThatTypeChecking("r=(1:0):5").hasError("Incompatible types.*RANGE but right is INT");
  }

  @Test
  public void badVariableRange() {
    assertThatTypeChecking("a=1.0 r=a:4").hasError("Cannot apply : operator to.*DOUBLE");
    assertThatTypeChecking("a=4.0 r=1:a").hasError("Incompatible types.*INT but right is DOUBLE");
    assertThatTypeChecking("s=0:1 r=s:5").hasError("Incompatible types.*RANGE but right is INT");
  }

  @Test
  public void constantStringSlice() {
    checkProgram("b='abcde' a=b[2:4]");
  }

  @Test
  public void variableStringSlice() {
    checkProgram("r=1:3 b='abcde' c=b[r]");
  }

  @Test
  public void stringSliceBadDescending() {
    assertThatTypeChecking("b='abcde' a=b[4:2]").hasError("must be non-descending; was 4:2");
  }

  @Test
  public void stringSliceBadNegative() {
    assertThatTypeChecking("b='abcde' a=b[-1:4]").hasError("must be non-negative; was -1");
  }

  @Test
  public void stringSliceBad() {
    assertThatTypeChecking("b='abcde' a=b[b:4]").hasError("Cannot apply : operator");
    assertThatTypeChecking("b='abcde' a=b[4:b]").hasError("Incompatible types");
    assertThatTypeChecking("b='abcde' a=b[4:4.0]").hasError("Incompatible types");
  }

  @Test
  public void arraySliceBad() {
    // can't take a slice of an array yet
    assertThatTypeChecking("b=[1,2,3,4] a=b[0:2]").hasError("was RANGE");
  }

  @Test
  public void stringLiteral_index() {
    SymbolTable symTab = checkProgram("a='hi'[1]");
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void arrayIndexMismatch() {
    assertThatTypeChecking("arr=[1,2,3] a=arr['bye']")
        .hasError("Index of ARRAY variable 'arr' must be INT; was STRING");
    assertThatTypeChecking("arr=[1,2,3] a=arr[false]").hasError("must be INT; was BOOL");
    assertThatTypeChecking("arr=[1,2,3] b='hi' a=arr[b]").hasError("must be INT; was STRING");
  }

  @Test
  public void arrayIndex() {
    SymbolTable symTab = checkProgram("arr=[1,2,3] a=arr[1]");
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void arrayString() {
    SymbolTable symTab = checkProgram("arr=['a', 'b', 'c'] a=arr[1 + 1]");
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void arrayLiteral() {
    SymbolTable symTab = checkProgram("a=[1,2,3]");
    assertThat(symTab.get("a").varType()).isArray();
  }

  @Test
  public void arrayLiteralMismatch() {
    assertThatTypeChecking("a=[1,true]").hasError("Inconsistent type");
    assertThatTypeChecking("b=3 a=[true,b]").hasError("Inconsistent type");
    assertThatTypeChecking("a=[true,b]").hasError("Indeterminable type");
    assertThatTypeChecking("a:int[2] a[0]=[1,2]")
        .hasError("declared as ARRAY of INT but.*ARRAY of INT");
  }

  @Test
  public void arrayLiteralGood() {
    SymbolTable symTab = checkProgram("f:proc():int {return 1} a=[1,f()]");
    VarType varType = symTab.get("a").varType();
    assertThat(varType).isArray();
    assertThat(varType).hasArrayBaseType(VarType.INT);
  }

  @Test
  public void arrayLiteral_index() {
    SymbolTable symTab = checkProgram("a=[1,2,3][1]"); // NO idea if this will work!
    assertThat(symTab.get("a").varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void arrayOperatorErrors(@TestParameter({"+", "-", "/", "%"}) String c) {
    assertThatTypeChecking(String.format("a1 = [1,2,3] %s [2,3,4]", c))
        .hasError(" to ARRAY expression");
  }

  @Test
  public void arraySet() {
    SymbolTable symTab = checkProgram("a:int[1] a[0]=1");
    assertThat(symTab.get("a").varType()).isArray();

    checkProgram("b=3 a:int[1] a[b+1]=1");
  }

  @Test
  public void arrayDeclError() {
    assertThatTypeChecking("a:int[-1]").hasError("must be non-negative; was -1");
    assertThatTypeChecking("a:int[true]").hasError("must be INT; was BOOL");
    assertThatTypeChecking("a:int[] b:int[] b=a").hasError("before assignment");
    assertThatTypeChecking("a:int[] a:int").hasError("already declared");
    assertThatTypeChecking("a:int[] a=3").hasError("Cannot convert");
    assertThatTypeChecking("a:int[] b=a").hasError("used before assignment");
  }

  @Test
  public void arrayDecl() {
    checkProgram("a:int[1]");
    checkProgram("a:int[]");
    checkProgram("a:int[1] b:int[] b=a");
    checkProgram("a:int[1] println a==null");
  }

  @Test
  public void arraySetTypeError() {
    assertThatTypeChecking("a:int[1] a[0]='hi'").hasError("declared as ARRAY of INT but.*STRING");
    assertThatTypeChecking("a:bool[1] a[0]=3").hasError("declared as ARRAY of BOOL but.*INT");
    assertThatTypeChecking("a:string[1] a[0]=true")
        .hasError("declared as ARRAY of STRING but.*BOOL");
    assertThatTypeChecking("a[0]=true").hasError("Unknown variable 'a' used as ARRAY");
    assertThatTypeChecking("a=3 a[0]=1").hasError("used as ARRAY; was INT");
  }

  @Test
  public void arraySetIndexError() {
    assertThatTypeChecking("a:int[1] a['hi']=1").hasError("ARRAY index must be INT; was STRING");
    assertThatTypeChecking("a:int[1] a[true]=1").hasError("ARRAY index must be INT; was BOOL");
    assertThatTypeChecking("b='hi' a:int[1] a[b]=1")
        .hasError("ARRAY index must be INT; was STRING");
    assertThatTypeChecking("b=true a:int[1] a[b]=1").hasError("ARRAY index must be INT; was BOOL");
    assertThatTypeChecking("a:int[1] a[-1]=1").hasError("ARRAY index must be non-negative; was -1");
  }

  @Test
  public void arrayGetIndexError() {
    assertThatTypeChecking("a:int[1] print a['hi']").hasError("must be INT; was STRING");
    assertThatTypeChecking("a:int[1] print a[true]").hasError("must be INT; was BOOL");
    assertThatTypeChecking("a:int[1] print a[-1]").hasError("must be non-negative; was -1");
  }

  @Test
  public void stringIndex_error() {
    assertThatTypeChecking("b='hi' a=b['bye']").hasError("must be INT or RANGE; was STRING");
    assertThatTypeChecking("b='hi' a=b[false]").hasError("must be INT or RANGE; was BOOL");
    assertThatTypeChecking("b='hi' a='hi'[b]").hasError("must be INT or RANGE; was STRING");
    assertThatTypeChecking("b='hi' a='hi'[-1]").hasError("must be non-negative; was -1");
    assertThatTypeChecking("b=3 a=b[3]")
        .hasError("Cannot apply \\[ operator to left operand of type INT");
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
    assertThatTypeChecking("if 1 { print 2 }").hasError("must be BOOL");
    assertThatTypeChecking("a=1 if a { print a }").hasError("must be BOOL");
  }

  @Test
  public void if_notBoolNested_error() {
    assertThatTypeChecking("a=1 if a==1 { if (a==1) { if b {print a } } }").hasError("UNKNOWN");
  }

  @Test
  public void if_error() {
    assertThatTypeChecking("a=1 if a==1 { a=b }").hasError("Indeterminable");
  }

  @Test
  public void if_duplicated_cases() {
    assertThatTypeChecking("a=1 if a==1 { print a } elif a == 1 {print 'a'} ")
        .hasError("Duplicate expression a == 1");
    assertThatTypeChecking("a='hi' if a==input { print a } elif a == input {print 'a'} ")
        .hasError("Duplicate expression a == INPUT");
    assertThatTypeChecking("a=1 if a==1 OR a == 2 { print a } elif a == 1 OR a == 2 {print 'a'} ")
        .hasError("Duplicate expression a == 1 OR a == 2");
  }

  @Test
  public void else_error() {
    assertThatTypeChecking("a=1 if a==1 {} else {a=b }").hasError("Indeterminable");
  }

  @Test
  public void main() {
    SymbolTable types = checkProgram("a=3 b=a c=b+4 d=b==c e=3<4 f=d==true");

    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookupRecursive("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of c").that(types.lookupRecursive("c")).isEqualTo(VarType.INT);
    assertWithMessage("type of d").that(types.lookupRecursive("d")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of e").that(types.lookupRecursive("e")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of f").that(types.lookupRecursive("f")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void whileLoop() {
    SymbolTable types = checkProgram("i=0 while i < 30 do b = i == 1 { print i }");

    assertWithMessage("type of i").that(types.lookupRecursive("i")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookupRecursive("b")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void while_errors() {
    assertThatTypeChecking("while a { print a }").hasError("UNKNOWN");
    assertThatTypeChecking("while 1 { print 1 }").hasError("INT");
    assertThatTypeChecking("while true do i = false + 1 {}").hasError("Cannot apply");
    assertThatTypeChecking("while true {i = false + 1}").hasError("Cannot apply");
  }

  @Test
  public void decl_errors() {
    assertThatTypeChecking("b=3 a=b a:int").hasError("already declared as INT");
    assertThatTypeChecking("a=3 a:bool").hasError("already declared as INT");
    assertThatTypeChecking("a:bool a:int").hasError("already declared as BOOL");
    assertThatTypeChecking("a:bool a:bool").hasError("already declared as BOOL");
    // This may or may not be an error later
    assertThatTypeChecking("a:int b=a").hasError("'a' used before assign");
    assertThatTypeChecking("a:int a=true").hasError("declared type INT to BOOL");
    assertThatTypeChecking("a:string a=true").hasError("declared type STRING to BOOL");
    assertThatTypeChecking("a:int a=''").hasError("declared type INT to STRING");
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
    SymbolTable types = checkProgram("a:int a=3 b=a");
    assertWithMessage("type of a").that(types.lookupRecursive("a")).isEqualTo(VarType.INT);
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
            + " nth = 0 "
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
    assertThatTypeChecking("fib:proc(n1):int {return n1}")
        .hasError("Indeterminable type for RETURN statement");
    assertThatTypeChecking("fib:proc(a, b, a) {}").hasError("Duplicate parameter");
    assertThatTypeChecking("fib:proc() {a=3 a=true}").hasError("declared type INT to BOOL");
    assertThatTypeChecking("a=true fib:proc() {a=3}").hasError("declared type BOOL to INT");
    assertThatTypeChecking("fib:proc(n1) {}").hasError("determine type of parameter");
    assertThatTypeChecking("fib:proc(n:int) {} fib(true)").hasError("found BOOL, expected INT");
    assertThatTypeChecking("fib:proc(a:int[]) {a[0]=3} fib(1)")
        .hasError("expected 1-d ARRAY of INT");
    assertThatTypeChecking("fib:proc(a:int) {a=3} fib([1])").hasError("found 1-d ARRAY of INT");
    assertThatTypeChecking("fib:proc(n1:rec){} ").hasError("unknown RECORD type rec");
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
    assertThatTypeChecking("fib:extern proc(n1) ").hasError("determine type of parameter");
    assertThatTypeChecking("fib:extern proc(n1:rec) ").hasError("unknown RECORD type rec");
    assertThatTypeChecking("fib:extern proc(a, b, a)").hasError("Duplicate parameter");
    assertThatTypeChecking("fib:extern proc(n:int) fib(true)").hasError("found BOOL, expected INT");
    assertThatTypeChecking("fib:extern proc(a:int[]) fib(1)").hasError("expected 1-d ARRAY of INT");
    assertThatTypeChecking("fib:extern proc(a:int) fib([1])").hasError("found 1-d ARRAY of INT");
  }

  @Test
  public void return_mismatch() {
    assertThatTypeChecking("fib:proc():bool {return 3}")
        .hasError("declared to return BOOL but RETURN statement was of type INT");
    assertThatTypeChecking("fib:proc(a):int {a='hi' return a}").hasError("INT.*STRING");
    assertThatTypeChecking("fib:proc(a:int) {a=3 return a}").hasError("VOID.*INT");

    assertThatTypeChecking("fib:proc() {return 3}").hasError("VOID.*INT");
    assertThatTypeChecking("fib:proc():int {return}").hasError("INT.*VOID");
  }

  @Test
  public void return_required() {
    assertThatTypeChecking("fib:proc():int {}").hasError("Not all codepaths");
    assertThatTypeChecking("fib:proc():bool { if false { return false } }")
        .hasError("Not all codepaths");
    assertThatTypeChecking("fib:proc():bool {"
        + "if false {"
        + "  if true {"
        + "    return false"
        + "  } elif false {"
        + "    return true"
        + "  } else {"
        + "    print 'hi'"
        + "  }"
        + "}"
        + "}").hasError("Not all codepaths");
    assertThatTypeChecking("fib:proc():bool {if false {return false} else {print 'hi'}}")
        .hasError("Not all codepaths");
    assertThatTypeChecking("fob:proc():int {"
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
        + "}").hasError("Not all codepaths");
    assertThatTypeChecking("      head:proc{}\r\n"
        + "head=[1]\r\n"
        + "bar:proc:int{\r\n"
        + "  if true {\r\n"
        + "    return head[0]\r\n"
        + "  }"
        + "}").hasError("Not all codepaths");
  }

  @Test
  public void callErrors() {
    assertThatTypeChecking("foo(3)").hasError("PROC 'foo' is undefined");
    assertThatTypeChecking("a:int a(3)").hasError("PROC 'a' is undefined");
    assertThatTypeChecking("fib:proc(){inner:proc(){}} inner(3)")
        .hasError("PROC 'inner' is undefined");
    // wrong number of params
    assertThatTypeChecking("fib:proc(){} fib(3)")
        .hasError("Wrong number of arguments in call to PROC 'fib': found 1, expected 0");
    assertThatTypeChecking("fib:proc(n:int){} fib(3, 4)")
        .hasError("Wrong number of arguments in call to PROC 'fib': found 2, expected 1");
    // indeterminable arg type
    assertThatTypeChecking("fib:proc(n) {fib(n)}")
        .hasError("Indeterminable type for parameter 'n' of PROC 'fib'");
    // wrong arg type
    assertThatTypeChecking("fib:proc(n:int) {} fib(false)")
        .hasError("Incorrect type of parameter 'n' to PROC 'fib': found BOOL, expected INT");
    // can't assign to void
    assertThatTypeChecking("fib:proc(n:int) {} x=fib(3)")
        .hasError("Cannot assign value of VOID expression");
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
    assertThatTypeChecking("exit -1").hasError("must be STRING");
    assertThatTypeChecking("exit length('sorry')").hasError("must be STRING");
  }

  @Test
  public void input() {
    checkProgram("f=input");
    checkProgram("f=input a=f+' should work'");
  }

  @Test
  public void input_errors() {
    assertThatTypeChecking("f:int f=input").hasError("declared type INT to STRING");
    assertThatTypeChecking("f=5 f=input").hasError("declared type INT to STRING");
  }

  @Test
  public void recordDefinition_empty() {
    SymbolTable symTab = checkProgram("r: record{}");
    assertThat(symTab.getRecursive("r")).isInstanceOf(RecordSymbol.class);
  }

  @Test
  public void recordDefinition_simple() {
    SymbolTable symTab = checkProgram("r2: record{s:string i:int b:bool}");
    RecordSymbol recordSymbol = symTab.getRecursive("r2", RecordSymbol.class);
    assertThat(recordSymbol).isNotNull();
    assertThat(recordSymbol.isGeneric()).isFalse();
  }

  @Test
  public void recordDefinition_generic() {
    SymbolTable symTab = checkProgram("r2: record<T, U> {s:T i:U b:bool}");
    RecordSymbol record = symTab.getRecursive("r2", RecordSymbol.class);
    assertThat(record).isNotNull();
    assertThat(record.isGeneric()).isTrue();
  }

  @Test
  public void recordDefinition_recursive() {
    SymbolTable symTab = checkProgram("rec: record{r:rec}");
    assertThat(symTab.getRecursive("rec", RecordSymbol.class)).isNotNull();
  }

  @Test
  public void recordDefinition_forward() {
    SymbolTable symTab = checkProgram("rec1: record{r:rec2} rec2: record{i:int}");
    assertThat(symTab.getRecursive("rec1", RecordSymbol.class)).isNotNull();
    assertThat(symTab.getRecursive("rec2", RecordSymbol.class)).isNotNull();
  }

  @Test
  public void recordDefinition_corecursive() {
    SymbolTable symTab = checkProgram("rec1: record{r:rec2} rec2: record{r:rec1}");
    assertThat(symTab.getRecursive("rec1", RecordSymbol.class)).isNotNull();
    assertThat(symTab.getRecursive("rec2", RecordSymbol.class)).isNotNull();
  }

  @Test
  public void recordDefinition_errors() {
    assertThatTypeChecking("r: record{i:int f:int f:bool i:int b:bool}")
        .hasError("Duplicate field\\(s\\) 'f, i' declared in RECORD 'r'");
    assertThatTypeChecking("r: record{f:dne}").hasError("unknown RECORD type dne");
    assertThatTypeChecking("s=3 r:record{a:string[s]} anr=new r print anr.a")
        .hasError("ARRAYs in RECORDs must have constant size");
    assertThatTypeChecking("r:record{a:string[1+1]} anr=new r print anr.a")
        .hasError("ARRAYs in RECORDs must have constant size");
    assertThatTypeChecking("r:record{as:string[1]} anr=new r aa=anr.as x=3 x=aa[0]")
        .hasError("declared type INT to STRING");
    assertThatTypeChecking("r: record{i:int f:int f:bool i:int b:bool}")
        .hasError("Duplicate field\\(s\\) 'f, i' declared in RECORD 'r'");
    assertThatTypeChecking("r: record{f:dne}").hasError("unknown RECORD type dne");
    assertThatTypeChecking("s=3 r:record{a:string[s]} anr=new r print anr.a")
        .hasError("ARRAYs in RECORDs must have constant size");
    assertThatTypeChecking("r:record{a:string[1+1]} anr=new r print anr.a")
        .hasError("ARRAYs in RECORDs must have constant size");
    assertThatTypeChecking("r:record{as:string[1]} anr=new r aa=anr.as x=3 x=aa[0]")
        .hasError("declared type INT to STRING");
  }

  @Test
  public void recordDefinition_duplicate() {
    assertThatTypeChecking("r: record{f:int} r:record{b:bool}")
        .hasError("'r' already declared as r: RECORD");
  }

  @Test
  public void recordDefinition_redeclaredAsRecord() {
    assertThatTypeChecking("r: int r:record{b:bool}").hasError("redeclared as INT");
  }

  @Test
  public void recordDefinition_redeclaredAsInt() {
    assertThatTypeChecking("r:record{b:bool} r: int").hasError("redeclared as INT");
  }

  @Test
  public void recordDefinition_redeclaredInProc() {
    assertThatTypeChecking("      f:proc{\n"
        + "  r: int \n"
        + "  r:record{b:bool}\n"
        + "}\n").hasError("redeclared as INT");
  }

  @Test
  public void recordDefinition_redeclaredInIf() {
    assertThatTypeChecking("if true {r: int r:record{b:bool}}").hasError("redeclared as INT");
    assertThatTypeChecking("if true {r:record{b:bool} r: int}").hasError("redeclared as INT");
  }

  @Test
  public void recordDefinition_error_duplicateName() {
    assertThatTypeChecking("r: record{f:int} r:record{b:bool}")
        .hasError("'r' already declared as r: RECORD");
  }

  @Test
  public void recordDefinition_error_redeclaredAsRecord() {
    assertThatTypeChecking("r: int r:record{b:bool}").hasError("redeclared as INT");
  }

  @Test
  public void recordDefinition_error_redeclaredAsInt() {
    assertThatTypeChecking("r:record{b:bool} r: int").hasError("redeclared as INT");
  }

  @Test
  public void recordDefinition_error_redeclaredInProc() {
    assertThatTypeChecking("      f:proc{\n"
        + "  r: int \n"
        + "  r:record{b:bool}\n"
        + "}\n").hasError("redeclared as INT");
  }

  @Test
  public void recordDefinition_error_redeclaredInIf() {
    assertThatTypeChecking("if true {r: int r:record{b:bool}}").hasError("redeclared as INT");
    assertThatTypeChecking("if true {r:record{b:bool} r: int}").hasError("redeclared as INT");
  }

  @Test
  public void recordDefinition_error_repeatedGeneric() {
    assertThatTypeChecking("r2: record<T, S, S, T, U> {s:T b:bool}")
        .hasError("Duplicate formal type\\(s\\) 'S, T'");
  }

  @Test
  public void recordUseGeneric() {
    assertThatTypeChecking("r2: record<T> {i:T} anr2=new r2<int> x:int x=anr2.i").succeeds();
  }

  @Test
  public void variableDecl_recordType() {
    SymbolTable symTab = checkProgram("r2: record{s:string} instance: r2");
    assertThat(symTab.getRecursive("r2", RecordSymbol.class)).isNotNull();

    Symbol rec = symTab.get("instance");
    assertThat(rec.isAssigned()).isFalse();
    RecordReferenceType refType = (RecordReferenceType) rec.varType();
    assertThat(refType.name()).isEqualTo("r2");
  }

  @Test
  public void variableDecl_errorRecordType() {
    assertThatTypeChecking("instance: r1")
        .hasError("Cannot declare variable 'instance' as unknown RECORD type r1");
  }

  @Test
  public void variableDecl_recordLikeTypeButNotQuiteRecordType() {
    assertThatTypeChecking("p:proc(){} instance: p")
        .hasError("Cannot declare variable 'instance' as unknown RECORD type p");
  }

  @Test
  public void paramDecl_recordType() {
    SymbolTable symTab = checkProgram("r1: record{s:string} p:proc(instance:r1){}");
    ProcSymbol proc = (ProcSymbol) symTab.get("p");
    ParamSymbol instance = proc.formal(0);
    RecordReferenceType r1 = (RecordReferenceType) instance.varType();
    assertThat(r1.name()).isEqualTo("r1");
  }

  @Test
  public void paramDecl_errorRecordType() {
    assertThatTypeChecking("p:proc(instance:r1){}")
        .hasError("Cannot declare variable 'instance' as unknown RECORD type r1");
  }

  @Test
  public void returnType_recordType() {
    SymbolTable symTab = checkProgram("r1: record{s:string} p:proc:r1{return null}");
    ProcSymbol proc = (ProcSymbol) symTab.get("p");
    RecordReferenceType r1 = (RecordReferenceType) proc.returnType();
    assertThat(r1.name()).isEqualTo("r1");
  }

  @Test
  public void returnType_errorRecordType() {
    assertThatTypeChecking("p:proc:r1 {return null}")
        .hasError("Cannot declare variable 'return type' as unknown RECORD type r1");
  }

  @Test
  public void assignRecordType() {
    SymbolTable symTab = checkProgram("r1:record{s:string} var1:r1 var1=null var2:r1 var2=var1");
    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType = (RecordReferenceType) var1.varType();
    assertThat(refType.name()).isEqualTo("r1");

    Symbol var2 = symTab.get("var2");
    RecordReferenceType refType2 = (RecordReferenceType) var2.varType();
    assertThat(refType2.name()).isEqualTo("r1");
  }

  @Test
  public void recordType_compare() {
    SymbolTable symTab =
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
    SymbolTable symTab =
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
    assertThatTypeChecking("s='' if null > s { print 'not null'}")
        .hasError("Cannot apply > operator to left operand of type NULL");
    assertThatTypeChecking("if null < null { print 'not null'}")
        .hasError("Cannot apply < operator to left operand of type NULL");
    assertThatTypeChecking("if not null {print 'not null'}")
        .hasError("Cannot apply NOT operator to NULL expression");
    assertThatTypeChecking("if null == 3 {print 'not 3'}")
        .hasError("Incompatible types for.*NULL.*INT");
  }

  @Test
  public void assignRecordType_inferred() {
    SymbolTable symTab = checkProgram("r1:record{s:string} var1:r1 var1=null var2=var1");
    System.err.println(symTab);
    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType = (RecordReferenceType) var1.varType();
    assertThat(refType.name()).isEqualTo("r1");

    Symbol var2 = symTab.get("var2");
    RecordReferenceType refType2 = (RecordReferenceType) var2.varType();
    assertThat(refType2.name()).isEqualTo("r1");
  }

  @Test
  public void assignRecordType_mismatch() {
    assertThatTypeChecking("r1:record{} var1:r1 var1=null var2:int var2=var1")
        .hasError("to r1: RECORD");
    assertThatTypeChecking("r1:record{} r2:record{} var1:r1 var1=null var2:r2 var2=var1")
        .hasError("to r1: RECORD");
    assertThatTypeChecking("r1:record{} r2:record{} var1:r1 var1=null var2:r2 var2=null var1=var2")
        .hasError("to r2: RECORD");
    assertThatTypeChecking("r1:record{} r2:record{} var1:r1 var1=null var2:r2 var2=null var1=var2")
        .hasError("to r2: RECORD");
    assertThatTypeChecking("r1:record{} r2:record{} var1:r1 var1=null var2:r2 var2=null var2=var1")
        .hasError("to r1: RECORD");
  }

  @Test
  public void assignRecordType_procReturnMismatch() {
    assertThatTypeChecking("r1:record{i:int} r2:record{} p:proc():r1{return new r2}")
        .hasError("but RETURN statement was of type r2: RECORD");
    assertThatTypeChecking(
        "r1:record{i:int} r2:record{} p:proc():r2{return new r2} var1:r1 var1=p()")
        .hasError("type r1: RECORD to r2: RECORD");
  }

  @Test
  public void newRecord() {
    SymbolTable symTab = checkProgram("r1:record{s:string} var1=new r1");
    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType = (RecordReferenceType) var1.varType();
    assertThat(refType.name()).isEqualTo("r1");
    assertThat(var1.isAssigned()).isTrue();
  }

  @Test
  public void newRecord_assign() {
    SymbolTable symTab = checkProgram("r1:record{s:string} var1=new r1 var2=var1");

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
    SymbolTable symTab = checkProgram("r1:record{i:int} p:proc():r1{return new r1} var1=p()");

    Symbol var1 = symTab.get("var1");
    RecordReferenceType refType1 = (RecordReferenceType) var1.varType();
    assertThat(refType1.name()).isEqualTo("r1");
    assertThat(var1.isAssigned()).isTrue();
  }

  @Test
  public void newRecord_unknown() {
    assertThatTypeChecking("var1=new r2").hasError("unknown RECORD type r2");
    assertThatTypeChecking("r1:record{s:string} var1=new r2").hasError("unknown RECORD type r2");
  }

  @Test
  public void newRecord_mismatch() {
    assertThatTypeChecking(
        "r1:record{s:string} r2:record{s:string} var1=new r1 var2 = new r2 var2=var1")
        .hasError("to r1: RECORD");
    assertThatTypeChecking(
        "r1:record{s:string} r2:record{s:string} var1=new r1 var2 = new r2 var1=var2")
        .hasError("to r2: RECORD");
    assertThatTypeChecking("r1:record{s:string} var1=new r1 var2=1 var1=var2").hasError("to INT");
    assertThatTypeChecking("r1:record{s:string} var1=new r1 var2=1 var2=var1")
        .hasError("to r1: RECORD");
  }

  @Test
  public void newRecord_generic() {
    SymbolTable symTab = checkProgram("Rec:record<T>{s:T} var=new Rec<int> var1=new Rec<Double>");

    Symbol var = symTab.get("var");
    RecordReferenceType refType = (RecordReferenceType) var.varType();
    assertThat(refType.name()).isEqualTo("Rec");
    assertThat(refType.actualTypes()).containsExactly(VarType.INT);

    var = symTab.get("var1");
    refType = (RecordReferenceType) var.varType();
    assertThat(refType.name()).isEqualTo("Rec");
    assertThat(refType.actualTypes()).containsExactly(VarType.DOUBLE);
  }

  @Test
  public void newRecord_wrongNumberOfGenerics() {
    assertThatTypeChecking("r1:record<T>{s:string} var1=new r1")
        .hasError("Wrong number.*saw 0, expected 1");
    assertThatTypeChecking("r1:record{s:string} var1=new r1<int>")
        .hasError("Wrong number.*saw 1, expected 0");
    assertThatTypeChecking("r1:record<T>{s:string} var1=new r1<R>")
        .hasError("unknown RECORD type R");
    assertThatTypeChecking("r1:record<S,T>{s:string} var1=new r1<int>")
        .hasError("Wrong number.*saw 1, expected 2");
  }

  @Test
  public void fieldGet() {
    SymbolTable symTab = checkProgram("r1:record{s:string i:int} var1=new r1 ii=var1.i");
    Symbol symbol = symTab.get("ii");
    assertThat(symbol.isAssigned()).isTrue();
    assertThat(symbol.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void fieldGet_nested() {
    SymbolTable symTab =
        checkProgram("r1:record{i:int} r2:record{rone:r1} var2=new r2 ii=var2.rone.i");
    Symbol symbol = symTab.get("ii");
    assertThat(symbol.isAssigned()).isTrue();
    assertThat(symbol.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void fieldGet_recursive() {
    SymbolTable symTab = checkProgram("r1:record{i:int next:r1} var1=new r1 var2=var1.next");

    Symbol var2 = symTab.get("var2");
    RecordReferenceType refType2 = (RecordReferenceType) var2.varType();
    assertThat(refType2.name()).isEqualTo("r1");
    assertThat(var2.isAssigned()).isTrue();
  }

  @Test
  public void fieldGet_procReturn() {
    SymbolTable symTab = checkProgram("r1:record{i:int} p:proc():r1{return new r1} var1=p().i");

    Symbol var1 = symTab.get("var1");
    assertThat(var1.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void fieldGet_mismatch() {
    assertThatTypeChecking("r1:record{s:string i:int} var1=new r1 ss:string ss=var1.i")
        .hasError("STRING to INT");
    assertThatTypeChecking("r1:record{s:string i:int} var1=new r1 ss='string' ss=var1.i")
        .hasError("STRING to INT");
    assertThatTypeChecking("ss='string' ss2=ss.i")
        .hasError("Cannot apply . operator to left operand of type STRING");
  }

  @Test
  public void record_badOp() {
    assertThatTypeChecking("r1:record{s:string i:int} var1=new r1 var2=var1+1")
        .hasError("Incompatible types for operator +");
    assertThatTypeChecking("r1:record{s:string i:int} var1=new r1 var2=1+var1")
        .hasError("Incompatible types for operator +");
  }

  @Test
  public void fieldSet() {
    SymbolTable symTab = checkProgram("r1:record{i:int} var1=new r1 var1.i=3 var2=var1.i");
    Symbol symbol = symTab.get("var2");
    assertThat(symbol.isAssigned()).isTrue();
    assertThat(symbol.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void fieldSet_error() {
    assertThatTypeChecking("r1:record{i:int} var1=new r1 var1.i=true").hasError("is BOOL");
    assertThatTypeChecking("r1:record{i:int s:string} var1=new r1 var1.i=var1.s")
        .hasError("is STRING");
    assertThatTypeChecking("r1:record{s:string} var1=new r1 var1.s=0").hasError("is INT");
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
    assertThatTypeChecking("      r:record{a:string} " //
        + "ar:r[2] "
        + "ai:int[2] "
        + "ar=ai").hasError("ARRAY of r to 1-d ARRAY of INT");
    assertThatTypeChecking("      r:record{a:string} " //
        + "ar:r[2] "
        + "ai:int[2] "
        + "ai=ar").hasError("ARRAY of INT to 1-d ARRAY of r");
  }

  @Test
  public void advancedRValue() {
    SymbolTable symtab =
        checkProgram(
            "      r1:record{bar:r2} r2:record{baz:r3[1]} r3:record{qux:string}"
                + " foo:r1[3]"
                + " a=4"
                + " f:proc:int{return 1}"
                + " bam = foo[3+a].bar.baz[f()].qux");
    assertThat(symtab.lookupRecursive("bam")).isEqualTo(VarType.STRING);
  }

  @Test
  public void invalidFieldName() {
    assertThatTypeChecking("r1:record{field:string} foo=new r1 bam = foo.3")
        .hasError("Cannot use expression 3 to get field of RECORD type r1");
    assertThatTypeChecking("r1:record{field:string} foo=new r1 bam = foo.'hi'")
        .hasError("Cannot use expression 'hi' to get field of RECORD type r1");
    assertThatTypeChecking("r1:record{field:string} foo=new r1 bam = foo.'field'")
        .hasError("Cannot use expression 'field' to get field of RECORD type r1");
    assertThatTypeChecking("r1:record{field:string} foo=new r1 bam = foo.true")
        .hasError("true to get field of RECORD type r1");
    assertThatTypeChecking("r1:record{field:string} foo=new r1 f='field' bam = foo.f")
        .hasError("unknown field f");
  }

  @Test
  public void noArgs() {
    SymbolTable symTab = checkProgram("a=3");
    assertThat(symTab.lookupRecursive("ARGS")).isEqualTo(VarType.UNKNOWN);
  }

  @Test
  public void simpleArgs() {
    SymbolTable symTab = checkProgram("println args[0]");
    assertThat(symTab.lookupRecursive("ARGS")).isArray();
    assertThat(symTab.lookupRecursive("ARGS")).hasArrayBaseType(VarType.STRING);
  }

  @Test
  public void copyArgs() {
    SymbolTable symTab = checkProgram("b=args");
    assertThat(symTab.lookupRecursive("b")).isArray();
    assertThat(symTab.lookupRecursive("b")).hasArrayBaseType(VarType.STRING);
  }

  @Test
  public void complexArgs() {
    SymbolTable symTab =
        checkProgram(
            "      len=length(args)\r\n"
                + "print 'length is ' println len\r\n"
                + "a=args[0]\r\n"
                + "println 'first is ' + a\r\n");
    assertThat(symTab.lookupRecursive("len")).isEqualTo(VarType.INT);
    assertThat(symTab.lookupRecursive("a")).isEqualTo(VarType.STRING);
    assertThat(symTab.lookupRecursive("ARGS")).isArray();
    assertThat(symTab.lookupRecursive("ARGS")).hasArrayBaseType(VarType.STRING);
  }

  @Test
  public void badArgs() {
    assertThatTypeChecking("a:bool a=args").hasError("Cannot convert");
    assertThatTypeChecking("a:bool a=args[0]").hasError("Cannot convert");
    assertThatTypeChecking("a:bool a=length(args)").hasError("Cannot convert");
  }

  @Test
  public void unknownWhile() {
    // Tests bug #204
    assertThatTypeChecking("while x < 3 { println x}")
        .hasError("Indeterminable type for expression x");
    assertThatTypeChecking("while 3 < x { println x}")
        .hasError("Indeterminable type for expression x");
  }

  @Test
  public void unknownBracket() {
    // Tests bug #205
    assertThatTypeChecking("x=a[3]").hasError("Indeterminable type for expression a");
    assertThatTypeChecking("p:proc() {x=a[3]}").hasError("Indeterminable type for expression a");
  }

  @Test
  public void badBracket() {
    // Tests bug #205
    assertThatTypeChecking("a=0 x=a[3]").hasError("Cannot apply.*operand of type INT");
  }

  @Test
  public void alreadyDeclaredAsProc() {
    // tests bug #214
    assertThatTypeChecking("head:proc{} head=[1] bar:proc:int{return head[0]}")
        .hasError("already declared as PROC");
    assertThatTypeChecking("head:proc{} r:record{} head=new r")
        .hasError("already declared as PROC");
    assertThatTypeChecking("head:proc{} head.f=3")
        .hasError("Cannot dereference.*already declared as PROC");
    assertThatTypeChecking("head:proc{} foo:proc {head[1]=3}").hasError("used as ARRAY; was PROC");
  }

  @Test
  public void badUnary() {
    // Tests bug #217
    assertThatTypeChecking("if not a { print 'sorry'}")
        .hasError("Indeterminable type for expression a");
  }

  @Test
  public void badIncDec() {
    assertThatTypeChecking("a++").hasError("type is unknown");
    assertThatTypeChecking("a=1.0 a++").hasError("already declared as DOUBLE");
    assertThatTypeChecking("a=1.0 a--").hasError("already declared as DOUBLE");
    assertThatTypeChecking("a=true a++").hasError("already declared as BOOL");
    assertThatTypeChecking("a=true a--").hasError("already declared as BOOL");
    assertThatTypeChecking("a='' a++").hasError("already declared as STRING");
    assertThatTypeChecking("a='' a--").hasError("already declared as STRING");
  }

  @Test
  public void goodIncDec() {
    checkProgram("a=1 a++");
    checkProgram("a=1 a--");
    checkProgram("a=1L a++");

    State state = assertThatTypeChecking("a=0y1 a--").succeeds();
    StatementNode node = state.programNode().statements().statements().get(1);
    assertThat(node).isInstanceOf(IncDecNode.class);
    assertThat(node.varType()).isEqualTo(VarType.BYTE);
  }

  @Test
  public void scopes() throws Exception {
    assertThatTypeChecking(""
        + "f:proc(flag:bool): string {\n"
        + "   if flag { s = 'hi'}\n"
        + "   return s\n" // this should be a typecheck error because 's' is in the inner block
        + "}\n"
        + "println f(false)\n").hasError("Indeterminable type for RETURN");
  }

  @Test
  public void bug_269_variable_with_proc_name() throws Exception {
    assertThatTypeChecking("r: record{} r:proc{} r=new r").hasError("already declared as PROC");
  }

  private static SymbolTable checkProgram(String program) {
    return firstSymTab(assertThatTypeChecking(program).succeeds());
  }

  private static SymbolTable firstSymTab(State state) {
    SymbolTable firstBlockSymTab =
        state.symbolTable().enterBlock(state.programNode().statements()).symTab();
    return firstBlockSymTab;
  }
}
