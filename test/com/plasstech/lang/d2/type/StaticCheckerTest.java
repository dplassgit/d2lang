package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.AssignmentNode;
import com.plasstech.lang.d2.parse.node.BinOpNode;
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.parse.node.UnaryNode;
import com.plasstech.lang.d2.parse.node.VariableNode;
import com.plasstech.lang.d2.parse.node.VariableSetNode;

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
    Lexer lexer = new Lexer("a=3");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignUnaryIntConst() {
    Lexer lexer = new Lexer("a=-3");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignUnaryVar() {
    Lexer lexer = new Lexer("a=3 b=-a");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(1);
    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    UnaryNode unaryNode = (UnaryNode) expr;
    assertThat(unaryNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignUnaryExpr() {
    Lexer lexer = new Lexer("a=3 b=-(a+3)");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(1);
    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    UnaryNode unaryNode = (UnaryNode) expr;
    assertThat(unaryNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignBool() {
    Lexer lexer = new Lexer("a=true");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.BOOL);

    Node expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void manyBinOps() {
    SymTab types = checkProgram("a=4 b=5  e=(a>=3)or not (b<3)");

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of e").that(types.lookup("e")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void assignBoolConstantUnaryError() {
    assertError("a=-true", "MINUS");
  }

  @Test
  public void assignBoolUnaryError() {
    assertError("a=true b=-a", "MINUS");
  }

  @Test
  public void assignIntUnaryOK() {
    SymTab types = checkProgram("a=3 b=!a");
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
  }

  @Test
  public void lengthNotStringFailure() {
    assertError("a=length(false)", "Cannot apply LENGTH");
    assertError("a=length(3)", "must take STRING");
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
    assertError("a=asc(3)", "must take STRING");
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
    assertError("a=chr('hi')", "must take INT");
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
    Lexer lexer = new Lexer("a=3+4-9");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.statements().statements().get(0);
    VariableSetNode var = (VariableSetNode) node.variable();
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
    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("b");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    VariableNode rhsNode = (VariableNode) expr;
    assertThat(rhsNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void assignMismatch() {
    assertError("a=true b=3 b=a", "declared as INT");
    assertError("a=3 b=true b=a", "declared as BOOL");
  }

  @Test
  public void declAssignMismatch() {
    assertError("a:int b=true a=b", "declared as INT");
    assertError("a:bool b=a b=3", "used before assignment");
    assertError("a=3 a:bool", "already declared as INT");
  }

  @Test
  public void declArray() {
    checkProgram("a:int[3]");
    checkProgram("b=3 a:int[b]");
    checkProgram("b:proc():int {return 0} a:string[b()]");
  }

  @Test
  public void declArrayMismatch() {
    assertError("a:int[b]", "Indeterminable type for array size; must be INT");
    assertError("a:int[false]", "Array size must be INT; was BOOL");
    assertError("a:int['hi']", "Array size must be INT; was STRING");
    assertError("a:string['hi']", "Array size must be INT; was STRING");
    assertError("b:proc() {} a:string[b()]", "Array size must be INT; was VOID");
    // this fails in an unexpected way ("used before assignment")
    // assertExecuteError("b:proc() {} a:string[b]", "Array size must be INT; was PROC");
  }

  @Test
  public void binOpMismatch() {
    for (String op : ImmutableList.of("==", "!=")) {
      assertError(String.format("a=true %s 3", op), "Type mismatch");
      assertError(String.format("a='hi' %s 3", op), "Type mismatch");
    }
  }

  @Test
  public void goodBooleanBinOp() {
    for (String op : ImmutableList.of("==", "or", "and", "<", ">")) {
      checkProgram(String.format("a=true %s false", op));
    }
  }

  @Test
  public void badBooleanBinOp() {
    for (String op : ImmutableList.of(">=", "<=")) {
      assertError(String.format("a=true %s false", op), "Cannot apply");
      assertError(String.format("a=true %s 3", op), "Cannot apply");
    }
  }

  @Test
  public void badBooleanSingleCharMismatch() {
    for (char c : "+-|&".toCharArray()) {
      assertError(String.format("a=true %c 3", c), "Cannot apply");
    }
  }

  @Test
  public void badStringSingleCharMismatch() {
    for (char c : "-|&".toCharArray()) {
      assertError(String.format("a='hi' %c 3", c), "Cannot apply");
    }
    assertError("a='hi' + 3", "Type mismatch");
  }

  @Test
  public void binOpStringInvalidOperator() {
    for (char c : "|&-%*/".toCharArray()) {
      assertError(String.format("a='hi' %c 'not'", c), "Cannot apply");
    }
  }

  @Test
  public void binOpStringValidOperator() {
    for (String op : ImmutableList.of("+", "<", ">", "==", "!=", "<=", ">=")) {
      checkProgram(String.format("a='hi' %s 'bye'", op));
    }
  }

  @Test
  public void binOpStringComparatorBoolean() {
    SymTab symTab = checkProgram("b:bool b='hi' == 'bye'");
    assertThat(symTab.get("b").type()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void binStringAdd() {
    SymTab symTab = checkProgram("b='hi' a='bye' c=a+b");
    assertThat(symTab.get("a").type()).isEqualTo(VarType.STRING);
    assertThat(symTab.get("b").type()).isEqualTo(VarType.STRING);
    assertThat(symTab.get("c").type()).isEqualTo(VarType.STRING);
  }

  @Test
  public void stringIndex() {
    SymTab symTab = checkProgram("b='hi' a=b[1]");
    assertThat(symTab.get("a").type()).isEqualTo(VarType.STRING);
  }

  @Test
  public void stringIndexConstant() {
    SymTab symTab = checkProgram("a='hi'[1]");
    assertThat(symTab.get("a").type()).isEqualTo(VarType.STRING);
  }

  @Test
  public void badArrayIndex() {
    assertError("arr=[1,2,3] b='hi' a=arr['bye']", "ARRAY index must be INT");
    assertError("arr=[1,2,3] b='hi' a=arr[false]", "ARRAY index must be INT");
    assertError("arr=[1,2,3] b='hi' a=arr[b]", "ARRAY index must be INT");
  }

  @Test
  public void arrayIndex() {
    SymTab symTab = checkProgram("arr=[1,2,3] a=arr[1]");
    assertThat(symTab.get("a").type()).isEqualTo(VarType.INT);
  }

  @Test
  public void arrayStringIndex() {
    SymTab symTab = checkProgram("arr=['a', 'b', 'c'] a=arr[1 + 1]");
    assertThat(symTab.get("a").type()).isEqualTo(VarType.STRING);
  }

  @Test
  public void arrayIndexConstant() {
    SymTab symTab = checkProgram("a=[1,2,3][1]"); // NO idea if this will work!
    assertThat(symTab.get("a").type()).isEqualTo(VarType.INT);
  }

  @Test
  public void badArrayOperators() {
    for (char c : "+-/%".toCharArray()) {
      assertError(
          String.format("a1 = [1,2,3] %c [2,3,4]", c), "operator to ARRAY expression");
    }
  }

  @Test
  public void badStringIndex() {
    assertError("b='hi' a=b['bye']", "STRING index must be INT");
    assertError("b='hi' a=b[false]", "STRING index must be INT");
    assertError("b='hi' a='hi'[b]", "STRING index must be INT");
    assertError("b=3 a=b[3]", "Cannot apply LBRACKET operator to INT expression");
  }

  @Test
  public void ifElifElse() {
    checkProgram("a=1 if a==1 { print a } elif a == 2 {print 2} else {print 3}");
  }

  @Test
  public void ifBool() {
    checkProgram("a=true if a { print a }");
  }

  @Test
  public void ifNotBoolCond_error() {
    assertError("if 1 { print 2 }", "must be BOOL; was INT");
    assertError("a=1 if a { print a }", "must be BOOL; was INT");
  }

  @Test
  public void ifNotBoolCondNested_error() {
    assertError("a=1 if a==1 { if (a==1) { if b {print a } } }", "UNKNOWN");
  }

  @Test
  public void errorInIf() {
    assertError("a=1 if a==1 { a=b }", "Indeterminable");
  }

  @Test
  public void errorInElse() {
    assertError("a=1 if a==1 {} else {a=b }", "Indeterminable");
  }

  @Test
  public void mainError() {
    assertError("a=1 main { if a==1 {} else {a=b}}", "Indeterminable");
  }

  @Test
  public void main() {
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
    VariableSetNode var = (VariableSetNode) node.variable();
    assertThat(var.name()).isEqualTo("b");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    VariableNode rhsNode = (VariableNode) expr;
    assertThat(rhsNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void whileLoop() {
    Lexer lexer = new Lexer("i=0 while i < 30 do b = i == 1 { print i }");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of i").that(types.lookup("i")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void whileError() {
    assertError("while a { print a }", "UNKNOWN");
    assertError("while 1 { print 1 }", "INT");
    assertError("while true do i = false + 1 {}", "Cannot apply");
    assertError("while true {i = false + 1}", "Cannot apply");
  }

  @Test
  public void declError() {
    assertError("b=3 a=b a:int", "already declared as INT");
    assertError("a=3 a:bool", "already declared as INT");
    assertError("a:bool a:int", "already declared as BOOL");
    assertError("a:bool a:bool", "already declared as BOOL");
    // This may or may not be an error later
    assertError("a:bool main {a:int}", "already declared as BOOL");
    assertError("a:int b=a", "'a' used before assign");
    assertError("a:int a=true", "declared as INT");
    assertError("a:string a=true", "declared as STRING");
    assertError("a:int a=''", "declared as INT");
  }

  @Test
  public void globalDeclsAreNeverUndefined() {
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
    Lexer lexer = new Lexer("a:int a=3 b=a");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
  }

  @Test
  public void proc() {
    checkProgram("fib:proc(n1:int, n2:int) : int { n1=3 n2=n1 return n1}");
    checkProgram("fib:proc(n:int) : int { n=3 return n}");
    checkProgram("fib:proc() {a=3} a=true");
    checkProgram("a=true fib:proc() {a:int a=3} ");
    checkProgram("fib:proc(n:int) : int { n=3 return n}");
    checkProgram(
        "level1:proc() : bool { "
            + " level2:proc() : int  {n=3 return n}"
            + " return false"
            + "} level1()");
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
  public void procParams() {
    assertError("fib:proc(a, b, a) {}", "Duplicate parameter");
    assertError("fib:proc() {a=3 a=true}", "declared as INT");
    assertError("a=true fib:proc() {a=3}", "declared as BOOL");
    assertError("fib:proc(n1) { }", "determine type of formal parameter");
    assertError("fib:proc(n:int) {} fib(true)", "found BOOL, expected INT");
  }

  @Test
  public void procMismatch() {
    assertError("fib:proc():bool {return 3}", "declared to return BOOL but returned INT");
    assertError(
        "fib:proc(a):int {a='hi' return a}", "declared to return INT but returned STRING");
    assertError(
        "fib:proc(a:int) {a=3 return a}", "declared to return VOID but returned INT");

    assertError("fib:proc() {return 3}", "declared to return VOID but returned INT");
    assertError("fib:proc():int {return}", "declared to return INT but returned VOID");
  }

  @Test
  public void procReturn() {
    assertError("fib:proc():int {}", "No RETURN statement");
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
    assertError(
        "fib:proc():bool {if false {return false} else {print 'hi'}}", "Not all codepaths");
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
  public void nakedReturn() {
    assertError("return 3", "outside a PROC");
  }

  @Test
  public void callErrors() {
    assertError("foo(3)", "PROC 'foo' is unknown");
    assertError("a:int a(3)", "PROC 'a' is unknown");
    assertError("fib:proc(){inner:proc(){}} inner(3)", "PROC 'inner' is unknown");
    // wrong number of params
    assertError(
        "fib:proc(){} fib(3)", "Wrong number of arguments to PROC 'fib': found 1, expected 0");
    assertError(
        "fib:proc(n:int){} fib(3, 4)",
        "Wrong number of arguments to PROC 'fib': found 2, expected 1");
    // indeterminable arg type
    assertError(
        "fib:proc(n) {fib(n)}", "Indeterminable type for parameter 'n' of PROC 'fib'");
    // wrong arg type
    assertError(
        "fib:proc(n:int) {} fib(false)",
        "Type mismatch for parameter 'n' to PROC 'fib': found BOOL, expected INT");
    // can't assign to void
    assertError("fib:proc(n:int) {} x=fib(3)", "Cannot assign value of void expression");
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
  public void bad_exit() {
    assertError("exit -1", "must be STRING");
    assertError("exit length('sorry')", "must be STRING");
  }

  @Test
  public void input() {
    checkProgram("f=input");
    checkProgram("f=input a=f+' should work'");
  }

  @Test
  public void bad_input() {
    assertError("f:int f=input", "declared as INT");
    assertError("f=5 f=input", "declared as INT");
  }

  @Test
  public void emptyRecord() {
    SymTab symTab = checkProgram("r: record{}");
    assertThat(symTab.get("r")).isInstanceOf(RecordSymbol.class);
  }

  @Test
  public void simpleRecord() {
    SymTab symTab = checkProgram("r2: record{s:string i:int b:bool}");
    assertThat(symTab.get("r2")).isInstanceOf(RecordSymbol.class);
  }

  @Test
  public void recursiveRecord() {
    SymTab symTab = checkProgram("rec: record{r:rec}");
    assertThat(symTab.get("rec")).isInstanceOf(RecordSymbol.class);
  }

  @Test
  public void badRecord() {
    assertError("r: record{f:record{f2:int}}", "nested RECORD 'f' in RECORD 'r'");
    assertError("r: record{p:proc() {} }", "nested PROC 'p' in RECORD 'r'");
    assertError(
        "r: record{i:int f:int f:bool i:int b:bool}",
        "Duplicate field(s) '[f, i]' declared in RECORD 'r'");
  }

  @Test
  public void duplicateRecord() {
    assertError("r: record{f:int} r:record{b:bool}", "'r' already declared as r: RECORD");
  }

  @Test
  public void redeclaredRecord() {
    assertError("r: int r:record{b:bool}", "already declared as INT");
  }

  private void assertError(String program, String messageShouldContain) {
    Lexer lexer = new Lexer(program);
    Parser parser = new Parser(lexer);
    Node rootNode = parser.parse();
    assertWithMessage("Should have passed parse for:\n " + program)
        .that(rootNode.isError())
        .isFalse();
    ProgramNode root = (ProgramNode) rootNode;
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertWithMessage("Should have result error for:\n " + program).that(result.isError()).isTrue();
    assertWithMessage("Should have correct error for:\n " + program)
        .that(result.message())
        .contains(messageShouldContain);
  }

  private SymTab checkProgram(String program) {
    Lexer lexer = new Lexer(program);
    Parser parser = new Parser(lexer);
    Node node = parser.parse();
    assertWithMessage("Should have passed parse for:\n " + program).that(node.isError()).isFalse();
    ProgramNode programRoot = (ProgramNode) node;
    StaticChecker checker = new StaticChecker(programRoot);
    SymTab symTab = execute(checker);
    return symTab;
  }

  private SymTab execute(StaticChecker checker) {
    TypeCheckResult result = checker.execute();
    assertWithMessage("Unexpected error: " + result.message()).that(result.isError()).isFalse();
    return result.symbolTable();
  }
}
