package com.plasstech.lang.d2.parse;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.parse.node.ArrayDeclarationNode;
import com.plasstech.lang.d2.parse.node.AssignmentNode;
import com.plasstech.lang.d2.parse.node.BinOpNode;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.BreakNode;
import com.plasstech.lang.d2.parse.node.CallNode;
import com.plasstech.lang.d2.parse.node.ConstNode;
import com.plasstech.lang.d2.parse.node.ContinueNode;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.ExprNode;
import com.plasstech.lang.d2.parse.node.IfNode;
import com.plasstech.lang.d2.parse.node.MainNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.PrintNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.parse.node.ReturnNode;
import com.plasstech.lang.d2.parse.node.StatementNode;
import com.plasstech.lang.d2.parse.node.UnaryNode;
import com.plasstech.lang.d2.parse.node.VariableNode;
import com.plasstech.lang.d2.parse.node.WhileNode;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.VarType;

public class ParserTest {
  @Test
  public void print() {
    BlockNode root = parseStatements("print 123");
    assertThat(root.statements()).hasSize(1);

    PrintNode node = (PrintNode) root.statements().get(0);
    assertThat(node.position().line()).isEqualTo(1);
    assertThat(node.position().column()).isEqualTo(1);
    assertThat(node.isPrintln()).isFalse();

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(123);
    assertThat(intNode.position().line()).isEqualTo(1);
    assertThat(intNode.position().column()).isEqualTo(7);
  }

  @Test
  public void println() {
    BlockNode root = parseStatements("println 123");
    assertThat(root.statements()).hasSize(1);

    PrintNode node = (PrintNode) root.statements().get(0);
    assertThat(node.isPrintln()).isTrue();
  }

  @Test
  public void printError() {
    assertParseError("Unexpected EOF", "print", "Unexpected EOF");
  }

  @Test
  public void assignErrors() {
    assertParseError("Missing expression", "a=", "expected literal");
    assertParseError("Missing close", "a=(3+", "expected literal");
    assertParseError("Missing close", "a=3+", "expected literal");
    assertParseError("Missing multiplicand", "a=3+5*", "expected literal");
    assertParseError("Missing multiplier", "a=3+*5", "expected literal");
    assertParseError("Missing add", "a=3**5", "expected literal");
    assertParseError("Missing expression", "a=print", "expected literal");
  }

  @Test
  public void assignInt() {
    BlockNode root = parseStatements("a=3");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(3);
  }

  @Test
  public void assignBoolean() {
    BlockNode root = parseStatements("a=true b=FALSE");
    assertThat(root.statements()).hasSize(2);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(((ConstNode<Boolean>) expr).value()).isTrue();

    node = (AssignmentNode) root.statements().get(1);
    expr = node.expr();
    assertThat(((ConstNode<Boolean>) expr).value()).isFalse();
  }

  @Test
  public void assignAdd() {
    BlockNode root = parseStatements("a=3 + 4");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();

    BinOpNode binOp = (BinOpNode) expr;
    ConstNode<Integer> left = (ConstNode<Integer>) binOp.left();
    assertThat(left.value()).isEqualTo(3);

    assertThat(binOp.operator()).isEqualTo(Token.Type.PLUS);

    ConstNode<Integer> right = (ConstNode<Integer>) binOp.right();
    assertThat(right.value()).isEqualTo(4);
  }

  @Test
  public void assignMult() {
    BlockNode root = parseStatements("a=3 * 4");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();

    BinOpNode binOp = (BinOpNode) expr;

    ConstNode<Integer> left = (ConstNode<Integer>) binOp.left();
    assertThat(left.value()).isEqualTo(3);

    assertThat(binOp.operator()).isEqualTo(Token.Type.MULT);

    ConstNode<Integer> right = (ConstNode<Integer>) binOp.right();
    assertThat(right.value()).isEqualTo(4);
  }

  @Test
  public void assignAddChained() {
    parseStatements("a=3+4*b-5");
  }

  @Test
  public void assign() {
    BlockNode root = parseStatements("a=b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    VariableNode atom = (VariableNode) expr;
    assertThat(atom.name()).isEqualTo("b");
  }

  @Test
  public void unaryMinus() {
    BlockNode root = parseStatements("a=-b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(Token.Type.MINUS);
    VariableNode right = (VariableNode) unary.expr();
    assertThat(right.name()).isEqualTo("b");
  }

  @Test
  public void unaryMinusConstant() {
    BlockNode root = parseStatements("a=-3");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ConstNode<Integer> expr = (ConstNode<Integer>) node.expr();
    assertThat(expr.value()).isEqualTo(-3);
  }

  @Test
  public void unaryPlusConstant() {
    BlockNode root = parseStatements("a=+3");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ConstNode<Integer> expr = (ConstNode<Integer>) node.expr();
    assertThat(expr.value()).isEqualTo(3);
  }

  @Test
  public void unaryNotConstant() {
    BlockNode root = parseStatements("a=!true");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ConstNode<Boolean> constNode = (ConstNode<Boolean>) node.expr();
    assertThat(constNode.value()).isFalse();
  }

  @Test
  public void unaryNot() {
    BlockNode root = parseStatements("a=!b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(((UnaryNode) expr).operator()).isEqualTo(Token.Type.NOT);
  }

  @Test
  public void unaryPlus() {
    BlockNode root = parseStatements("a=+b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(Token.Type.PLUS);
    VariableNode right = (VariableNode) unary.expr();
    assertThat(right.name()).isEqualTo("b");
  }

  @Test
  public void unaryExpr() {
    BlockNode root = parseStatements("a=+(b+-c)");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(Token.Type.PLUS);
  }

  @Test
  public void unaryNegativeInt() {
    assertUnaryAssignConstant("a=-5", -5);
    assertUnaryAssignConstant("a=-+5", -5);
    assertUnaryAssignConstant("a=+-5", -5);
    assertUnaryAssignConstant("a=---5", -5);
  }

  @Test
  public void multipleUnary() {
    assertUnaryAssignConstant("a=++5", 5);
    assertUnaryAssignConstant("a=+++5", 5);
    assertUnaryAssignConstant("a=--5", 5);
    assertUnaryAssignConstant("a=-+-5", 5);
    assertUnaryAssignConstant("a=--+5", 5);
  }

  private void assertUnaryAssignConstant(String expression, int value) {
    BlockNode root = parseStatements(expression);
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(((ConstNode<Integer>) expr).value()).isEqualTo(value);
  }

  @Test
  public void unaryLength() {
    BlockNode root = parseStatements("a=length('hi')");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(Token.Type.LENGTH);
    ConstNode<String> right = (ConstNode<String>) unary.expr();
    assertThat(right.value()).isEqualTo("hi");
  }

  @Test
  public void binopOperator() {
    for (char c : "+-*/%><|&".toCharArray()) {
      parseStatements(String.format("a=b%c5", c));
    }
  }

  @Test
  public void binopCompare() {
    for (String op : ImmutableList.of("==", "!=", "<=", ">=")) {
      parseStatements(String.format("a=b%s5", op));
    }
  }

  @Test
  public void allExprTypes_exceptAndOr() {
    // boolean a = ((1 + 2) * (3 - 4) / (-5) == 6) == true
    // || ((2 - 3) * (4 - 5) / (-6) == 7) == false && ((3 + 4) * (5 + 6) / (-7) >=
    // (8 % 2));
    BlockNode root = parseStatements("a=((1 + 2) * (3 - 4) / (-5) == 6) != true");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);
  }

  @Test
  public void allExprTypes() {
    BlockNode root = parseStatements("a=((1 + 2) * (3 - 4) / (-5) == 6) != true\n"
            + " | ((2 - 3) * (4 - 5) / (-6) < 7) == !false & \n"
            + " ((3 + 4) * (5 + 6) / (-7) >= (8 % 2))"
            + "b=1+2*3-4/5==6!=true|2-3*4-5/-6<7==!a & 3+4*5+6/-7>=8%2");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(2);
  }

  @Test
  public void program() {
    BlockNode root = parseStatements("a=3 print a\n abc =   123 +a-b print 123\nprin=t");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(5);

    assertThat(statements.get(0)).isInstanceOf(AssignmentNode.class);
    assertThat(statements.get(1)).isInstanceOf(PrintNode.class);
    assertThat(statements.get(2)).isInstanceOf(AssignmentNode.class);
    assertThat(statements.get(3)).isInstanceOf(PrintNode.class);
    assertThat(statements.get(4)).isInstanceOf(AssignmentNode.class);
  }

  @Test
  public void assignParens() {
    BlockNode root = parseStatements("a=(3)");

    assertThat(root.statements()).hasSize(1);
    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(3);
  }

  @Test
  public void parseIf() {
    BlockNode root = parseStatements("if a==3 { print a a=4 }");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    IfNode.Case first = ifNode.cases().get(0);
    assertThat(first.block().statements()).hasSize(2);
  }

  @Test
  public void ifEmptyBlock() {
    BlockNode root = parseStatements("if a==3 { }");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock()).isNull();
    IfNode.Case first = ifNode.cases().get(0);
    assertThat(first.block().statements()).isEmpty();
  }

  @Test
  public void ifNested() {
    BlockNode root = parseStatements("if a==3 { " + "if a==4 { " + " if a == 5 {" + "   print a"
            + " } " + "} }" + "else { print 4 print a}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock().statements()).hasSize(2);
  }

  @Test
  public void ifElse() {
    BlockNode root = parseStatements("if a==3 { print a } else { print 4 print a}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock().statements()).hasSize(2);
  }

  @Test
  public void ifElif() {
    BlockNode root = parseStatements("if a==3 { print a } elif a==4 { print 4 print a} "
            + "elif a==5 { print 5}else { print 6 print 7}");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(3);
    assertThat(ifNode.elseBlock()).isNotNull();
  }

  @Test
  public void ifError() {
    assertParseError("Missing open brace", "if a==3 { print a } else print 4}", "expected {");
    assertParseError("Missing close brace", "if a==3 { print a } else {print 4", "expected 'print");
    assertParseError("Missing open brace", "if a==3 print a } else {print 4", "expected {");
    assertParseError("Missing expression brace", "if print a else {print 4", "expected literal");
    assertParseError("Extra elif", "if a==3 { print a } else  { print 4 print a} "
            + "elif a==5 { print 5}else { print 6 print 7}", "Unexpected ELIF");
  }

  @Test
  public void whileTrue() {
    BlockNode root = parseStatements("while true {}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);
    assertThat(whileNode.doStatement().isPresent()).isFalse();
    ExprNode condition = whileNode.condition();
    assertThat(((ConstNode<Boolean>) condition).value()).isTrue();
    BlockNode block = whileNode.block();
    assertThat(block.statements()).isEmpty();
  }

  @Test
  public void whileDo() {
    BlockNode root = parseStatements("while true do i = 1 {}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);

    AssignmentNode assignment = (AssignmentNode) whileNode.doStatement().get();
    VariableNode var = assignment.variable();
    assertThat(var.name()).isEqualTo("i");

    ExprNode expr = assignment.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(1);
  }

  @Test
  public void whileExprDo() {
    BlockNode root = parseStatements("while i < 30 do i = 1 {}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);

    ExprNode condition = whileNode.condition();

    BinOpNode binOp = (BinOpNode) condition;
    VariableNode left = (VariableNode) binOp.left();
    assertThat(left.name()).isEqualTo("i");
    assertThat(binOp.operator()).isEqualTo(Token.Type.LT);
    ConstNode<Integer> right = (ConstNode<Integer>) binOp.right();
    assertThat(right.value()).isEqualTo(30);
  }

  @Test
  public void whileExprDoBlock() {
    BlockNode root = parseStatements("while i < 30 do i = 1 {print i a=i}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);

    BlockNode block = whileNode.block();
    assertThat(block.statements()).hasSize(2);
  }

  @Test
  public void whileBreak() {
    BlockNode root = parseStatements("while true {break continue}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    WhileNode whileNode = (WhileNode) statements.get(0);

    BlockNode block = whileNode.block();
    assertThat(block.statements()).hasSize(2);
    assertThat(block.statements().get(0)).isInstanceOf(BreakNode.class);
    assertThat(block.statements().get(1)).isInstanceOf(ContinueNode.class);
  }

  @Test
  public void whileDoNotAssignment() {
    parseStatements("while true do advance(3) {}");
  }

  @Test
  public void whileError() {
    assertParseError("Missing expression", "while print", "expected literal");
    assertParseError("Missing open brace", "while a==3 print", "expected {");
    assertParseError("Missing close brace", "while a==3 {print", "expected literal");
    assertParseError("Missing do assignment", "while a==3 do {print}", "expected 'print'");
    assertParseError("Bad do assignment", "while a==3 do print {print}", "expected literal");
    assertParseError("Bad statement", "while a==3 do a=a+1 {a=}", "expected literal");
    assertParseError("Unexpected continue", "continue", "CONTINUE keyword");
    assertParseError("Unexpected break", "break", "BREAK keyword");
    assertParseError("Unexpected break", "if true {break while true {continue }}", "BREAK keyword");
    assertParseError("Unexpected continue", "if true {continue while true {break}}",
            "CONTINUE keyword");
  }

  @Test
  public void mainEmpty() {
    ProgramNode root = parseProgram("main{}");
    assertThat(root.main().isPresent()).isTrue();
  }

  @Test
  public void statementThenMainEmpty() {
    ProgramNode root = parseProgram("print 123 main{}");
    BlockNode block = root.statements();
    assertThat(block.statements()).hasSize(1);

    PrintNode node = (PrintNode) block.statements().get(0);
    assertThat(node.position().line()).isEqualTo(1);
    assertThat(node.position().column()).isEqualTo(1);

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(123);
    assertThat(intNode.position().line()).isEqualTo(1);
    assertThat(intNode.position().column()).isEqualTo(7);
    assertThat(root.main().isPresent()).isTrue();
  }

  @Test
  public void mainWithStatement() {
    ProgramNode root = parseProgram("main{ print 123 }");
    BlockNode globalBlock = root.statements();
    assertThat(globalBlock.statements()).isEmpty();
    MainNode main = root.main().get();
    BlockNode block = main.block();

    PrintNode node = (PrintNode) block.statements().get(0);

    ExprNode expr = node.expr();
    ConstNode<Integer> intNode = (ConstNode<Integer>) expr;
    assertThat(intNode.value()).isEqualTo(123);
  }

  @Test
  public void mainError() {
    assertParseError("Missing open brace", "main", "expected {");
    assertParseError("Missing close brace", "main {", "expected 'print");
    assertParseError("Missing eof ", "main {} print ", "expected EOF");
    assertParseError("Missing eof ", "main { print ", "expected literal");
  }

  @Test
  public void decl() {
    BlockNode root = parseStatements("a:int");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.name()).isEqualTo("a");
    assertThat(declarationNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void declBool() {
    BlockNode root = parseStatements("a:bool");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.name()).isEqualTo("a");
    assertThat(declarationNode.varType()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void declString() {
    BlockNode root = parseStatements("a:string");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    DeclarationNode declarationNode = (DeclarationNode) statements.get(0);
    assertThat(declarationNode.name()).isEqualTo("a");
    assertThat(declarationNode.varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void declarationError() {
    assertParseError("Missing type", "a:", "expected INT, BOOL");
    assertParseError("Missing type", "a::", "expected INT, BOOL");
    assertParseError("Missing close brace", "a:print", "expected INT, BOOL");
  }

  @Test
  public void assignString() {
    BlockNode root = parseStatements("a='hi'");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.STRING);
  }

  @Test
  public void addStrings() {
    BlockNode root = parseStatements("a='hi' + 'Hi'");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    assertThat(expr).isInstanceOf(BinOpNode.class);
  }

  @Test
  public void procErrors() {
    assertParseError("Should not be allowed", "fib:proc(a:int b) {}", "expected , or )");
    assertParseError("Should not be allowed", "fib:proc(a:bad, b) {}", "expected INT");
    assertParseError("Should not be allowed", "fib:proc(a:, b) {}", "expected INT");
    assertParseError("Should not be allowed", "fib:proc(a: b) {}", "expected INT");
    assertParseError("Should not be allowed", "fib:proc(a:) {}", "expected INT");
    assertParseError("Should not be allowed", "fib:proc(a {}", "expected , or )");
    assertParseError("Should not be allowed", "fib:proc(a:int, ) {}", "expected variable");
    assertParseError("Should not be allowed", "fib:proc(a:int) print a", "expected {");
    assertParseError("Should not be allowed", "fib:proc  print a", "expected {");
    assertParseError("Should not be allowed", "fib:proc() {return", "Unexpected EOF");
    assertParseError("Should not be allowed", "fib:proc() {return {", "Unexpected {");
    assertParseError("Should not be allowed", "fib:proc() {return )}", "Unexpected )");
  }

  @Test
  public void simpleProc() {
    // the simplest possible procedure
    ProgramNode root = parseProgram("fib:proc {}");
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
  }

  @Test
  public void procWithParam() {
    ProgramNode root = parseProgram("fib:proc(param1) {}");
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.parameters()).hasSize(1);
    assertThat(proc.parameters().get(0).name()).isEqualTo("param1");
    assertThat(proc.parameters().get(0).type().isUnknown()).isTrue();
  }

  @Test
  public void procWith2Params() {
    ProgramNode root = parseProgram("fib:proc(param1, param2: string) {}");
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.parameters()).hasSize(2);
    assertThat(proc.parameters().get(0).name()).isEqualTo("param1");
    assertThat(proc.parameters().get(0).type().isUnknown()).isTrue();
    assertThat(proc.parameters().get(1).name()).isEqualTo("param2");
    assertThat(proc.parameters().get(1).type()).isEqualTo(VarType.STRING);
  }

  @Test
  public void procWithLocals() {
    ProgramNode root = parseProgram("fib:proc() {local:int}");
    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.block().statements().get(0)).isInstanceOf(DeclarationNode.class);
  }

  @Test
  public void fullProc() {
    ProgramNode root = parseProgram( //
            "fib:proc(typed:int, nontyped) : string {" //
                    + "typed = typed + 1" //
                    + "nontyped = typed + 1" //
                    + "return 'hi'" //
                    + "}"); //

    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.STRING);
    assertThat(proc.parameters()).hasSize(2);
    assertThat(proc.block().statements()).hasSize(3);
  }

  @Test
  public void procReturnVoid() {
    ProgramNode root = parseProgram("fib:proc() {return}");

    ProcedureNode proc = (ProcedureNode) (root.statements().statements().get(0));
    assertThat(proc.name()).isEqualTo("fib");
    assertThat(proc.returnType()).isEqualTo(VarType.VOID);
    assertThat(proc.block().statements()).hasSize(1);
    ReturnNode returnNode = (ReturnNode) proc.block().statements().get(0);
    assertThat(returnNode.expr().isPresent()).isFalse();

    // This is allowed, but the static checker will eventually prevent it
    parseProgram("fib:proc() {return print 'hi'}");
  }

  @Test
  public void procCallNoArgs() {
    ProgramNode root = parseProgram("a = doit()");
    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallAsStatement() {
    ProgramNode root = parseProgram("doit(3)");
    CallNode call = (CallNode) (root.statements().statements().get(0));

    assertThat(call.functionToCall()).isEqualTo("doit");
    assertThat(call.actuals()).hasSize(1);
    ExprNode param = call.actuals().get(0);
    assertThat(param.isSimpleType()).isTrue();
  }

  @Test
  public void procCallAsExpression() {
    ProgramNode root = parseProgram("a = doit((3*6*(3-4)*(5-5)), (abc==doit()))");

    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallOneArgs() {
    ProgramNode root = parseProgram("a = doit(1)");

    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallErrors() {
    assertParseError("Should fail", "a = doit(1 b=3", "expected ')");
    assertParseError("Should fail", "a = doit(1,)", "expected literal");
  }

  @Test
  public void procCallMultipleArgs() {
    ProgramNode root = parseProgram("a = doit(1, 2, 3)");
    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void procCallNested() {
    ProgramNode root = parseProgram("a = doit3(1, doit1(2), doit2(3, 4))");
    AssignmentNode assignment = (AssignmentNode) (root.statements().statements().get(0));
    ExprNode expr = assignment.expr();
    assertThat(expr).isInstanceOf(CallNode.class);
  }

  @Test
  public void arrayGet() {
    BlockNode root = parseStatements("a=b[3+c]");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    BinOpNode expr = (BinOpNode) node.expr();
    assertThat(expr.left()).isInstanceOf(VariableNode.class);
    assertThat(expr.operator()).isEqualTo(Token.Type.LBRACKET);
    assertThat(expr.right()).isInstanceOf(BinOpNode.class);
  }

  @Test
  public void arrayGetWeirdYetParseable() {
    parseStatements("a=3[4+c]"); // will fail type checker
    parseStatements("a='hi'['lol']"); // will fail type checker
    parseStatements("a=fn()[4+c]");
  }

  @Test
  public void arrayDecl() {
    BlockNode root = parseStatements("a:int[3]");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    ArrayDeclarationNode node = (ArrayDeclarationNode) statements.get(0);
    assertThat(node.name()).isEqualTo("a");
    assertThat(node.varType()).isInstanceOf(ArrayType.class);
    assertThat(node.sizeExpr()).isInstanceOf(ConstNode.class);

    ArrayType arrType = (ArrayType) node.varType();
    assertThat(arrType.baseType()).isEqualTo(VarType.INT);
  }

  @Test
  public void arrayDeclVariableSize() {
    BlockNode root = parseStatements("a:int[b+3]");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    ArrayDeclarationNode node = (ArrayDeclarationNode) statements.get(0);
    assertThat(node.name()).isEqualTo("a");
    assertThat(node.varType()).isInstanceOf(ArrayType.class);
    assertThat(node.sizeExpr()).isInstanceOf(BinOpNode.class);

    ArrayType arrType = (ArrayType) node.varType();
    assertThat(arrType.baseType()).isEqualTo(VarType.INT);
  }

  @Test
  public void arrayDeclError() {
    assertParseError("Should not parse incomplete array decl", "a:int[3", "expected '['");
    assertParseError("Should not parse incomplete array decl", "a:int[3 4]", "expected '['");
    assertParseError("Should not parse bad base array type", "a:hello[3]", "expected INT");
  }

  @Test
  @Ignore("Issue #38: Support multidimensional arrays")
  public void multiDimArrayGet() {
    BlockNode root = parseStatements("a=b[3+c][4][5]");
    System.out.println(root);
  }

  @Test
  @Ignore("Assignments are still broken")
  public void arraySet() {
    parseStatements("a[3] = 4");
  }

  @Test
  @Ignore
  public void arrayStmt() {
    parseStatements("fn()[fn()]");
  }

  @Test
  public void arrayGetError() {
    assertParseError("No closing bracket", "a=3[4+c b", "expected ']'");
    assertParseError("No closing bracket", "a=3[4+c b]", "expected ']'");
  }

  @Test
  public void literalIntArray() {
    BlockNode root = parseStatements("a=[1,2,3]");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    @SuppressWarnings("unchecked")
    ConstNode<Integer[]> array = (ConstNode<Integer[]>) expr;
    assertThat(array.value()[0]).isEqualTo(1);
  }

  @Test
  public void literalStringArray() {
    BlockNode root = parseStatements("a=['one', 'two', 'three']");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    @SuppressWarnings("unchecked")
    ConstNode<String[]> array = (ConstNode<String[]>) expr;
    assertThat(array.value()[0]).isEqualTo("one");
  }

  @Test
  public void literalBoolArray() {
    BlockNode root = parseStatements("a=[true, false]");
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    ExprNode expr = node.expr();
    @SuppressWarnings("unchecked")
    ConstNode<Boolean[]> array = (ConstNode<Boolean[]>) expr;
    assertThat(array.value()[0]).isTrue();
    assertThat(array.value()[1]).isFalse();
  }

  @Test
  public void literalArrayIndex() {
    BlockNode root = parseStatements("a=[true, false][1]");
    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    BinOpNode expr = (BinOpNode) node.expr();
    assertThat(expr.left()).isInstanceOf(ConstNode.class);
    assertThat(expr.right()).isInstanceOf(ConstNode.class);
  }

  @Test
  public void literalArrayErrors() {
    assertParseError("Should not parse", "a=[]", "Empty");
    assertParseError("Should not parse", "a=[a]", "only scalar");
    assertParseError("Should not parse", "a=[a+1]", "only scalar");
    assertParseError("Should not parse", "a=[1,'hi']", "Inconsistent types");
  }

  private BlockNode parseStatements(String expression) {
    ProgramNode node = parseProgram(expression);
    return node.statements();
  }

  private ProgramNode parseProgram(String expression) {
    Lexer lexer = new Lexer(expression);
    Parser parser = new Parser(lexer);
    Node node = parser.parse();
    if (node.isError()) {
      fail(node.message());
    }
    return (ProgramNode) node;
  }

  private void assertParseError(String message, String expressionToParse, String errorMsgContains) {
    Lexer lexer = new Lexer(expressionToParse);
    Parser parser = new Parser(lexer);
    Node node = parser.parse();
    assertWithMessage(message).that(node.isError()).isTrue();
    assertThat(node.message()).contains(errorMsgContains);
  }
}
