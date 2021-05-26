package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.ConstNode;
import com.plasstech.lang.d2.parse.ErrorNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.ProgramNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;

public class StaticCheckerTest {

  @Test
  public void execute_print() {
    checkProgram("print 123");
  }

  @Test
  public void execute_printUnassigned() {
    assertExecuteError("print a", "Indeterminable");
    assertExecuteError("print (1-3)*a", "Indeterminable");
  }

  @Test
  public void execute_assignInt() {
    Lexer lexer = new Lexer("a=3");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_assignUnaryIntConst() {
    Lexer lexer = new Lexer("a=-3");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_assignUnaryVar() {
    Lexer lexer = new Lexer("a=3 b=-a");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(1);
    VariableNode var = node.variable();
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    UnaryNode unaryNode = (UnaryNode) expr;
    assertThat(unaryNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_assignUnaryExpr() {
    Lexer lexer = new Lexer("a=3 b=-(a+3)");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(1);
    VariableNode var = node.variable();
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    UnaryNode unaryNode = (UnaryNode) expr;
    assertThat(unaryNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_assignBool() {
    Lexer lexer = new Lexer("a=true");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.BOOL);

    Node expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void execute_manybinops() {
    SymTab types = checkProgram("a=4 b=5  e=(a>=3)|!(b<3)");

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of e").that(types.lookup("e")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void execute_assignBoolConstantUnaryError() {
    assertExecuteError("a=-true", "MINUS");
  }

  @Test
  public void execute_assignBoolUnaryError() {
    assertExecuteError("a=true\nb=-a", "MINUS");
  }

  @Test
  public void execute_assignIntUnaryFailure() {
    assertExecuteError("a=3\nb=!a", "NOT");
  }

  @Test
  public void execute_assignBoolConstantUnary() {
    SymTab types = checkProgram("a=!true");

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void execute_assignBoolUnary() {
    SymTab types = checkProgram("a=true\nb=!a");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void execute_assignExpr() {
    Lexer lexer = new Lexer("a=3+4-9");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    BinOpNode binOpNode = (BinOpNode) expr;
    assertThat(binOpNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_assignExprIndeterminable() {
    assertExecuteError("a=3+(4-b)", "Indeterminable type");
  }

  @Test
  public void execute_assignExprIndeterminableMultiple() {
    assertExecuteError("a=3 b=a+3 c=d", "Indeterminable type");
  }

  @Test
  public void execute_assignMulti() {
    Lexer lexer = new Lexer("a=3 b=a c = b+4 d=b==c e=3<4 f=d==true print c");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of c").that(types.lookup("c")).isEqualTo(VarType.INT);
    assertWithMessage("type of e").that(types.lookup("e")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of f").that(types.lookup("f")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of d").that(types.lookup("d")).isEqualTo(VarType.BOOL);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(1);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("b");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    VariableNode rhsNode = (VariableNode) expr;
    assertThat(rhsNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_assignMismatch() {
    assertExecuteError("a=true b=3 b=a", "Type mismatch");
    assertExecuteError("a=3 b=true b=a", "Type mismatch");
  }

  @Test
  public void execute_declarationAssignMismatch() {
    assertExecuteError("a:int b=true a=b", "Type mismatch");
    assertExecuteError("a:bool b=a b=3", "used before assignment");
    assertExecuteError("a=3 a:bool", "already declared as INT");
  }

  @Test
  public void execute_binOpMismatch() {
    for (String op : ImmutableList.of("==", "!=", "<=", ">=")) {
      assertExecuteError(String.format("a=true %s 3", op), "Type mismatch");
      assertExecuteError(String.format("a='hi' %s 3", op), "Type mismatch");
    }
  }

  @Test
  public void execute_binOpSingleCharMismatch() {
    for (char c : "+-<>|&".toCharArray()) {
      assertExecuteError(String.format("a=true %c 3", c), "Type mismatch");
      assertExecuteError(String.format("a='hi' %c 3", c), "Type mismatch");
    }
  }

  @Test
  public void execute_binOpStringInvalidOperator() {
    for (char c : "|&-%*/".toCharArray()) {
      assertExecuteError(String.format("a='hi' %c 'not'", c), "Cannot apply");
    }
  }

  @Test
  public void execute_binOpStringValidOperator() {
    for (String op : ImmutableList.of("+", "<", ">", "==", "!=", "<=", ">=")) {
      checkProgram(String.format("a='hi' %s 'bye'", op));
    }
  }

  @Test
  public void execute_binOpStringComparatorBoolean() {
    SymTab symTab = checkProgram("b:bool b='hi' == 'bye'");
    assertThat(symTab.get("b").type()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void execute_binStringAdd() {
    SymTab symTab = checkProgram("b='hi' a='bye' c=a+b");
    assertThat(symTab.get("a").type()).isEqualTo(VarType.STRING);
    assertThat(symTab.get("b").type()).isEqualTo(VarType.STRING);
    assertThat(symTab.get("c").type()).isEqualTo(VarType.STRING);
  }

  @Test
  public void execute_ifElifElse() {
    checkProgram("a=1 if a==1 { print a } elif a == 2 {print 2} else {print 3}");
  }

  @Test
  public void execute_ifBool() {
    checkProgram("a=true if a { print a }");
  }

  @Test
  public void execute_ifNotBoolCond_error() {
    assertExecuteError("if 1 { print 2 }", "must be boolean; was INT");
    assertExecuteError("a=1 if a { print a }", "must be boolean; was INT");
  }

  @Test
  public void execute_ifNotBoolCondNested_error() {
    assertExecuteError("a=1 if a==1 { if (a==1) { if b {print a } } }", "UNKNOWN");
  }

  @Test
  public void execute_errorInIf() {
    assertExecuteError("a=1 if a==1 { a=b }", "Indeterminable");
  }

  @Test
  public void execute_errorInElse() {
    assertExecuteError("a=1 if a==1 {} else {a=b }", "Indeterminable");
  }

  @Test
  public void execute_mainError() {
    assertExecuteError("a=1 main { if a==1 {} else {a=b}}", "Indeterminable");
  }

  @Test
  public void execute_main() {
    Lexer lexer = new Lexer("main {a=3 b=a c=b+4 d=b==c e=3<4 f=d==true}");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of c").that(types.lookup("c")).isEqualTo(VarType.INT);
    assertWithMessage("type of d").that(types.lookup("d")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of e").that(types.lookup("e")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of f").that(types.lookup("f")).isEqualTo(VarType.BOOL);

    AssignmentNode node = (AssignmentNode) root.main().get().block().statements().get(1);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("b");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    VariableNode rhsNode = (VariableNode) expr;
    assertThat(rhsNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_while() {
    Lexer lexer = new Lexer("i=0 while i < 30 do b = i == 1 { print i }");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of i").that(types.lookup("i")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void execute_whileError() {
    assertExecuteError("while a { print a }", "UNKNOWN");
    assertExecuteError("while 1 { print 1 }", "INT");
    assertExecuteError("while true do i = false + 1{ }", "mismatch");
    assertExecuteError("while true { i = false + 1}", "mismatch");
  }

  @Test
  public void execute_declarationError() {
    assertExecuteError("b=3 a=b a:int", "already declared as INT");
    assertExecuteError("a=3 a:bool", "already declared as INT");
    assertExecuteError("a:bool a:int", "already declared as BOOL");
    assertExecuteError("a:bool a:bool", "already declared as BOOL");
    // This may or may not be an error later
    assertExecuteError("a:bool main {a:int}", "already declared as BOOL");
    assertExecuteError("a:int b=a", "'a' used before assign");
    assertExecuteError("a:int a=true", "mismatch");
    assertExecuteError("a:string a=true", "mismatch");
    assertExecuteError("a:int a=''", "mismatch");
  }

  @Test
  public void execute_declaration() {
    Lexer lexer = new Lexer("a:int a=3 b=a");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_procedure() {
    checkProgram("fib:proc(n1:int, n2) : int { n1=3 n2=n1 return n1}");
    checkProgram("fib:proc(n:int) : int { n=3 return n}");
    checkProgram("fib:proc() {a=3} a=true");
    checkProgram("a=true fib:proc() {a:int a=3} ");
    checkProgram("fib:proc(n) : int { n=3 return n}");
    checkProgram("level1:proc() : bool { " //
            + " level2:proc() : int  {n=3 return n}" //
            + " return false" //
            + "} level1()"); //
  }

  @Test
  public void execute_procedure_recursive() {
    checkProgram("fib:proc(n:int) : int {" //
            + "  if n <= 1 {" //
            + "    return n" //
            + "  } else {" //
            + "    return fib(n-1) + fib(n-2)" //
            + "  }" //
            + "}" //
            + "");
  }

  @Test
  public void execute_procedure_iterative() {
    checkProgram("fib2:proc (n:int) : int {" //
            + " n1 = 0 " //
            + " n2 = 1 " //
            + " i=1 while i < n do i = i + 1 { " //
            + "  nth = n1 + n2 " //
            + "  n1 = n2 " //
            + "  n2 = nth " //
            + " } " //
            + " return nth " //
            + "}");
  }

  @Test
  public void execute_procedureBadParams() {
    assertExecuteError("fib:proc(a, b, a) {}", "Duplicate parameter");
    assertExecuteError("fib:proc() {a=3 a=true}", "Type mismatch");
    assertExecuteError("a=true fib:proc() {a=3}", "Type mismatch");
    assertExecuteError("fib:proc(n1) { }", "determine type of parameter");
  }

  @Test
  public void execute_procedureReturnMismatch() {
    assertExecuteError("fib:proc():bool {return 3}", "Type mismatch");
    assertExecuteError("fib:proc(a):int {a='hi' return a}", "Type mismatch");
    assertExecuteError("fib:proc(a:int) {a=3 return a}", "Type mismatch");

    assertExecuteError("fib:proc() {return 3}", "Type mismatch");
    assertExecuteError("fib:proc():int {return}", "Type mismatch");
  }

  @Test
  public void execute_procedureNoReturn() {
    assertExecuteError("fib:proc():int {}", "No 'return' statement");
    assertExecuteError("fib:proc():bool {if false {return false}}", "Not all codepaths");
    assertExecuteError("fib:proc():bool {if false {return false} else {print 'hi'}}", "Not all codepaths");
    assertExecuteError("fib:proc():bool {if false {if true {return false} elif false {return true} else {print 'hi'}}}", "Not all codepaths");
  }

  @Test
  public void execute_nakedReturn() {
    assertExecuteError("return 3", "outside a procedure");
  }

  @Test
  public void execute_callErrors() {
    assertExecuteError("foo(3)", "Procedure foo is unknown");
    assertExecuteError("a:int a(3)", "Procedure a is unknown");
    assertExecuteError("fib:proc(){inner:proc(){}} inner(3)", "Procedure inner is unknown");
    // wrong number of params
    assertExecuteError("fib:proc(){} fib(3)",
            "Wrong number of arguments to procedure fib: found 1, expected 0");
    assertExecuteError("fib:proc(n:int){} fib(3, 4)",
            "Wrong number of arguments to procedure fib: found 2, expected 1");
    // indeterminable arg type
    assertExecuteError("fib:proc(n) {fib(n)}",
            "Indeterminable type for parameter n of procedure fib");
    // wrong arg type
    assertExecuteError("fib:proc(n:int) {} fib(false)",
            "Type mismatch for parameter n of procedure fib: found BOOL, expected INT");
    // can't assign to void
    assertExecuteError("fib:proc(n:int) {} x=fib(3)", "Cannot assign value of void expression");
  }

  @Test
  public void callInnerProc() {
    checkProgram("level1:proc() : bool { " //
            + " level2:proc() : int  {n=3 return n}" //
            + " m=level2()" //
            + " return m==3" //
            + "}"); //
  }

  private void assertExecuteError(String program, String messageShouldContain) {
    Lexer lexer = new Lexer(program);
    Parser parser = new Parser(lexer);
    Node rootNode = parser.parse();
    assertWithMessage("Should have passed parse for:\n " + program).that(rootNode.isError())
            .isFalse();
    ProgramNode root = (ProgramNode) rootNode;
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertWithMessage("Should have result error for:\n " + program).that(result.isError()).isTrue();
    System.err.println(result.message());
    assertThat(result.message()).contains(messageShouldContain);
  }

  private SymTab checkProgram(String program) {
    Lexer lexer = new Lexer(program);
    Parser parser = new Parser(lexer);
    Node node = parser.parse();
    if (node.isError()) {
      fail(((ErrorNode) node).message());
    }
    ProgramNode programRoot = (ProgramNode) node;
    System.out.println("Before: " + programRoot);
    StaticChecker checker = new StaticChecker(programRoot);
    SymTab symTab = execute(checker);
    System.out.println("After: " + programRoot);
    return symTab;
  }

  private SymTab execute(StaticChecker checker) {
    TypeCheckResult result = checker.execute();
    assertWithMessage("unexpected error: " + result.message()).that(result.isError()).isFalse();
    return result.symbolTable();
  }
}
