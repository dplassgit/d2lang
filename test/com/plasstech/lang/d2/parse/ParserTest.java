package com.plasstech.lang.d2.parse;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.lex.Token;

public class ParserTest {
  @Test
  public void parse_print() {
    BlockNode root = parseStatements("print 123");
    assertThat(root.statements()).hasSize(1);

    PrintNode node = (PrintNode) root.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.PRINT);
    assertThat(node.position().line()).isEqualTo(1);
    assertThat(node.position().column()).isEqualTo(1);

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.INT);
    IntNode intNode = (IntNode) expr;
    assertThat(intNode.value()).isEqualTo(123);
    assertThat(intNode.position().line()).isEqualTo(1);
    assertThat(intNode.position().column()).isEqualTo(7);
  }

  @Test
  public void parse_printErr() {
    Lexer lexer = new Lexer("print");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
    System.err.println(((ErrorNode) node).message());
  }

  @Test
  public void parse_assignErrors() {
    assertParseError("Missing expression", "a=", "expected literal");
    assertParseError("Missing close", "a=(3+", "expected literal");
    assertParseError("Missing close", "a=3+", "expected literal");
    assertParseError("Missing multiplicand", "a=3+5*", "expected literal");
    assertParseError("Missing multiplier", "a=3+*5", "expected literal");
    assertParseError("Missing add", "a=3**5", "expected literal");
    assertParseError("Missing expression", "a=print", "expected literal");
  }

  @Test
  public void parse_assignInt() {
    BlockNode root = parseStatements("a=3");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.INT);
    IntNode intNode = (IntNode) expr;
    assertThat(intNode.value()).isEqualTo(3);
  }

  @Test
  public void parse_assignTrueFalse() {
    BlockNode root = parseStatements("a=true b=FALSE");
    assertThat(root.statements()).hasSize(2);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.BOOL);
    assertThat(((BoolNode) expr).value()).isTrue();

    node = (AssignmentNode) root.statements().get(1);
    expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.BOOL);
    assertThat(((BoolNode) expr).value()).isFalse();
  }

  @Test
  public void parse_assignmentAdd() {
    BlockNode root = parseStatements("a=3 + 4");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.BIN_OP);

    BinOpNode binOp = (BinOpNode) expr;
    IntNode left = (IntNode) binOp.left();
    assertThat(left.value()).isEqualTo(3);
    assertThat(binOp.operator()).isEqualTo(Token.Type.PLUS);
    IntNode right = (IntNode) binOp.right();
    assertThat(right.value()).isEqualTo(4);
  }

  @Test
  public void parse_assignmentMul() {
    BlockNode root = parseStatements("a=3 * 4");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);

    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.BIN_OP);

    BinOpNode binOp = (BinOpNode) expr;
    IntNode left = (IntNode) binOp.left();
    assertThat(left.value()).isEqualTo(3);
    assertThat(binOp.operator()).isEqualTo(Token.Type.MULT);
    IntNode right = (IntNode) binOp.right();
    assertThat(right.value()).isEqualTo(4);
  }

  @Test
  public void parse_assignmentAddChained() {
    parseStatements("a=3+4*b-5");
  }

  @Test
  public void parse_assignment() {
    BlockNode root = parseStatements("a=b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.VARIABLE);
    VariableNode atom = (VariableNode) expr;
    assertThat(atom.name()).isEqualTo("b");
  }

  @Test
  public void parse_unaryMinus() {
    BlockNode root = parseStatements("a=-b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.UNARY);
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(Token.Type.MINUS);
    Node right = unary.expr();
    assertThat(right.nodeType()).isEqualTo(Node.Type.VARIABLE);
  }

  @Test
  public void parse_unaryNotConstant() {
    BlockNode root = parseStatements("a=!true");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.BOOL);
    assertThat(((BoolNode) expr).value()).isFalse();
  }

  @Test
  public void parse_unaryNot() {
    BlockNode root = parseStatements("a=!b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.UNARY);
    assertThat(((UnaryNode) expr).operator()).isEqualTo(Token.Type.NOT);
  }

  @Test
  public void parse_unaryPlus() {
    BlockNode root = parseStatements("a=+b");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.UNARY);
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(Token.Type.PLUS);
    Node right = unary.expr();
    assertThat(right.nodeType()).isEqualTo(Node.Type.VARIABLE);
  }

  @Test
  public void parse_unaryExpr() {
    BlockNode root = parseStatements("a=+(b+-c)");
    assertThat(root.statements()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.UNARY);
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.operator()).isEqualTo(Token.Type.PLUS);
    Node right = unary.expr();
    assertThat(right.nodeType()).isEqualTo(Node.Type.BIN_OP);
  }

  @Test
  public void parse_unaryNegativeInt() {
    assertUnaryAssignConstant("a=-5", -5);
    assertUnaryAssignConstant("a=-+5", -5);
    assertUnaryAssignConstant("a=+-5", -5);
    assertUnaryAssignConstant("a=---5", -5);
  }

  @Test
  public void parse_multipleUnary() {
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
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.INT);
    assertThat(((IntNode) expr).value()).isEqualTo(value);
  }

  @Test
  public void parse_each_binop_single() {
    for (char c : "+-*/%><|&".toCharArray()) {
      parseStatements(String.format("a=b%c5", c));
    }
  }

  @Test
  public void parse_each_binop_multiple() {
    for (String op : ImmutableList.of("==", "!=", "<=", ">=")) {
      parseStatements(String.format("a=b%s5", op));
    }
  }

  @Test
  public void parse_allExprTypes_exceptAndOr() {
    // boolean a = ((1 + 2) * (3 - 4) / (-5) == 6) == true
    // || ((2 - 3) * (4 - 5) / (-6) == 7) == false && ((3 + 4) * (5 + 6) / (-7) >=
    // (8 % 2));
    BlockNode root = parseStatements("a=((1 + 2) * (3 - 4) / (-5) == 6) != true\n");
//    System.out.println(root);
  }

  @Test
  public void parse_allExprTypes() {
    BlockNode root2 = parseStatements("a=((1 + 2) * (3 - 4) / (-5) == 6) != true\n"
            + " | ((2 - 3) * (4 - 5) / (-6) < 7) == !false & \n"
            + " ((3 + 4) * (5 + 6) / (-7) >= (8 % 2))"
            + "b=1+2*3-4/5==6!=true|2-3*4-5/-6<7==!a & 3+4*5+6/-7>=8%2");
//    System.out.println(root2);
  }

  @Test
  public void parse_program() {
    BlockNode root = parseStatements("a=3 print a\n abc =   123 +a-b print 123\nprin=t");
//    System.out.println(root);

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(5);

    assertThat(statements.get(0).nodeType()).isEqualTo(Node.Type.ASSIGNMENT);
    assertThat(statements.get(1).nodeType()).isEqualTo(Node.Type.PRINT);
    assertThat(statements.get(2).nodeType()).isEqualTo(Node.Type.ASSIGNMENT);
    assertThat(statements.get(3).nodeType()).isEqualTo(Node.Type.PRINT);
    assertThat(statements.get(4).nodeType()).isEqualTo(Node.Type.ASSIGNMENT);
  }

  @Test
  public void parse_assignmentParens() {
    BlockNode root = parseStatements("a=(3)");

    assertThat(root.statements()).hasSize(1);
    AssignmentNode node = (AssignmentNode) root.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.INT);
    IntNode intNode = (IntNode) expr;
    assertThat(intNode.value()).isEqualTo(3);
  }

  @Test
  public void parse_if() {
    BlockNode root = parseStatements("if a==3 { print a a=4 }");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    assertThat(statements.get(0).nodeType()).isEqualTo(Node.Type.IF);
    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    IfNode.Case first = ifNode.cases().get(0);
    assertThat(first.condition().nodeType()).isEqualTo(Node.Type.BIN_OP);
    assertThat(first.block().statements()).hasSize(2);
  }

  @Test
  public void parse_ifEmpty() {
    BlockNode root = parseStatements("if a==3 { }");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    assertThat(statements.get(0).nodeType()).isEqualTo(Node.Type.IF);
    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock()).isNull();
    IfNode.Case first = ifNode.cases().get(0);
    assertThat(first.condition().nodeType()).isEqualTo(Node.Type.BIN_OP);
    assertThat(first.block().statements()).isEmpty();
  }

  @Test
  public void parse_ifNested() {
    BlockNode root = parseStatements("if a==3 { " + "if a==4 { " + " if a == 5 {" + "   print a" + " } "
            + "} }" + "else { print 4 print a}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    assertThat(statements.get(0).nodeType()).isEqualTo(Node.Type.IF);
    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock().statements()).hasSize(2);
  }

  @Test
  public void parse_ifElse() {
    BlockNode root = parseStatements("if a==3 { print a } else { print 4 print a}");

    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    assertThat(statements.get(0).nodeType()).isEqualTo(Node.Type.IF);
    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(1);
    assertThat(ifNode.elseBlock().statements()).hasSize(2);
  }

  @Test
  public void parse_ifElif() {
    BlockNode root = parseStatements("if a==3 { print a } elif a==4 { print 4 print a} "
            + "elif a==5 { print 5}else { print 6 print 7}");
    System.out.println(root);
    List<StatementNode> statements = root.statements();
    assertThat(statements).hasSize(1);

    assertThat(statements.get(0).nodeType()).isEqualTo(Node.Type.IF);
    IfNode ifNode = (IfNode) statements.get(0);
    assertThat(ifNode.cases()).hasSize(3);
    assertThat(ifNode.elseBlock()).isNotNull();
  }

  @Test
  public void parse_ifError() {
    assertParseError("Missing open brace", "if a==3 { print a } else print 4}", "expected {");
    assertParseError("Missing close brace", "if a==3 { print a } else {print 4",
            "expected 'print");
    assertParseError("Missing open brace", "if a==3 print a } else {print 4", "expected {");
    assertParseError("Missing expression brace", "if print a else {print 4", "expected literal");
    assertParseError("Extra elif", "if a==3 { print a } else  { print 4 print a} "
            + "elif a==5 { print 5}else { print 6 print 7}", "Unexpected ELIF");
  }

  @Test
  public void parse_mainEmpty() {
    ProgramNode root = parseProgram("main{}");
    assertThat(root.main().isPresent()).isTrue();
  }

  @Test
  public void parse_statementThenMainEmpty() {
    ProgramNode root = parseProgram("print 123 main{}");
    BlockNode block = root.statements();
    assertThat(block.statements()).hasSize(1);

    PrintNode node = (PrintNode) block.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.PRINT);
    assertThat(node.position().line()).isEqualTo(1);
    assertThat(node.position().column()).isEqualTo(1);

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.INT);
    IntNode intNode = (IntNode) expr;
    assertThat(intNode.value()).isEqualTo(123);
    assertThat(intNode.position().line()).isEqualTo(1);
    assertThat(intNode.position().column()).isEqualTo(7);
    assertThat(root.main().isPresent()).isTrue();
  }

  @Test
  public void parse_mainWithStatement() {
    ProgramNode root = parseProgram("main{print 123 }");
    BlockNode globalBlock = root.statements();
    assertThat(globalBlock.statements()).isEmpty();
    MainNode main = root.main().get();
    BlockNode block = main.statements();

    PrintNode node = (PrintNode) block.statements().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.PRINT);

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.INT);
    IntNode intNode = (IntNode) expr;
    assertThat(intNode.value()).isEqualTo(123);
  }

  @Test
  public void parse_mainError() {
    assertParseError("Missing open brace", "main", "expected {");
    assertParseError("Missing close brace", "main {", "expected 'print");
    assertParseError("Missing eof ", "main {} print ", "expected EOF");
    assertParseError("Missing eof ", "main { print ", "expected literal");
  }

  private BlockNode parseStatements(String expression) {
    Node node = parseProgram(expression);
    if (node.isError()) {
      ErrorNode error = (ErrorNode) node;
      fail(error.message());
    }
    return ((ProgramNode) node).statements();
  }

  private ProgramNode parseProgram(String expression) {
    Lexer lexer = new Lexer(expression);
    Parser parser = new Parser(lexer);
    Node node = parser.parse();
    if (node.isError()) {
      ErrorNode error = (ErrorNode) node;
      fail(error.message());
    }
    return (ProgramNode) node;
  }

  private void assertParseError(String message, String expressionToParse, String errorMsgContains) {
    Lexer lexer = new Lexer(expressionToParse);
    Parser parser = new Parser(lexer);
    Node node = parser.parse();
    assertWithMessage(message).that(node.isError()).isTrue();
    ErrorNode error = (ErrorNode) node;
    System.err.println(error.message());
    assertThat(error.message()).contains(errorMsgContains);
  }
}
