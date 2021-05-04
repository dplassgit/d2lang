package com.plasstech.lang.d2.parse;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.util.List;

import org.junit.Test;

import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.lex.Token;


public class ParserTest {
  @Test
  public void testParse_print() {
    Lexer lexer = new Lexer("print 123");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    assertThat(root.children()).hasSize(1);

    PrintNode node = (PrintNode) root.children().get(0);
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
  public void testParse_printErr() {
    Lexer lexer = new Lexer("print");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
    System.err.println(((ErrorNode) node).message());
  }

  @Test
  public void testParse_assignIncomplete() {
    Lexer lexer = new Lexer("a=");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
    System.err.println(((ErrorNode) node).message());
    assertThat(((ErrorNode) node).message()).contains("EOF");
  }

  @Test
  public void testParse_missingCloseParens() {
    Lexer lexer = new Lexer("\na=(3+");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
    System.err.println(((ErrorNode) node).message());
    assertThat(((ErrorNode) node).message()).contains("EOF");
  }

  @Test
  public void testParse_addIncomplete() {
    Lexer lexer = new Lexer("a=3+");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
    System.err.println(((ErrorNode) node).message());
    assertThat(((ErrorNode) node).message()).contains("EOF");
  }

  @Test
  public void testParse_mulIncomplete() {
    Lexer lexer = new Lexer("a=3+5*");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
    System.err.println(((ErrorNode) node).message());
    assertThat(((ErrorNode) node).message()).contains("EOF");
  }

  @Test
  public void testParse_mulMissing() {
    Lexer lexer = new Lexer("a=3+*5");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
    System.err.println(((ErrorNode) node).message());
    assertThat(((ErrorNode) node).message()).contains("Found *");
  }

  @Test
  public void testParse_addMissing() {
    Lexer lexer = new Lexer("a=3**5");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
    System.err.println(((ErrorNode) node).message());
    assertThat(((ErrorNode) node).message()).contains("Found *");
  }

  @Test
  public void testParse_assignErrror() {
    Lexer lexer = new Lexer("a=print");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
    assertThat(((ErrorNode) node).message()).contains("PRINT");
    System.err.println(((ErrorNode) node).message());
  }

  @Test
  public void testParse_assignInt() {
    Lexer lexer = new Lexer("a=3");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    assertThat(root.children()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.children().get(0);

    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.INT);
    IntNode intNode = (IntNode) expr;
    assertThat(intNode.value()).isEqualTo(3);
  }

  @Test
  public void testParse_assignTrueFalse() {
    Lexer lexer = new Lexer("a=true b=FALSE");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    assertThat(root.children()).hasSize(2);

    AssignmentNode node = (AssignmentNode) root.children().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.BOOL);
    assertThat(((BoolNode) expr).value()).isTrue();

    node = (AssignmentNode) root.children().get(1);
    expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.BOOL);
    assertThat(((BoolNode) expr).value()).isFalse();
  }

  @Test
  public void testParse_assignmentAdd() {
    Lexer lexer = new Lexer("a=3   + 4");
    Parser parser = new Parser(lexer);

    Node rootNode = parser.parse();
    assertWithMessage(rootNode.toString()).that(rootNode.isError()).isFalse();

    StatementsNode root = (StatementsNode) rootNode;
    assertThat(root.children()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.children().get(0);

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
  public void testParse_assignmentMul() {
    Lexer lexer = new Lexer("a=3   * 4");
    Parser parser = new Parser(lexer);

    Node rootNode = parser.parse();
    assertWithMessage(rootNode.toString()).that(rootNode.isError()).isFalse();

    StatementsNode root = (StatementsNode) rootNode;
    assertThat(root.children()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.children().get(0);

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
  public void testParse_assignmentAddChained() {
    Lexer lexer = new Lexer("a=3+4*b-5");
    Parser parser = new Parser(lexer);

    Node rootNode = parser.parse();
    assertWithMessage(rootNode.toString()).that(rootNode.isError()).isFalse();
  }

  @Test
  public void testParse_assignment() {
    Lexer lexer = new Lexer("a=b");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    assertThat(root.children()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.children().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.VARIABLE);
    VariableNode atom = (VariableNode) expr;
    assertThat(atom.name()).isEqualTo("b");
  }

  @Test
  public void testParse_unaryMinus() {
    Lexer lexer = new Lexer("a=-b");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    assertThat(root.children()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.children().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.UNARY);
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.opType()).isEqualTo(Token.Type.MINUS);
    Node right = unary.expr();
    assertThat(right.nodeType()).isEqualTo(Node.Type.VARIABLE);
  }

  @Test
  public void testParse_unaryPlus() {
    Lexer lexer = new Lexer("a=+b");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    assertThat(root.children()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.children().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.UNARY);
    UnaryNode unary = (UnaryNode) expr;
    assertThat(unary.opType()).isEqualTo(Token.Type.PLUS);
    Node right = unary.expr();
    assertThat(right.nodeType()).isEqualTo(Node.Type.VARIABLE);
  }

  @Test
  public void testParse_unaryNegativeInt() {
    assertUnaryAssignConstant("a=-5", -5);
    assertUnaryAssignConstant("a=-+5", -5);
    assertUnaryAssignConstant("a=+-5", -5);
    assertUnaryAssignConstant("a=---5", -5);
  }

  @Test
  public void testParse_multipleUnary() {
    assertUnaryAssignConstant("a=++5", 5);
    assertUnaryAssignConstant("a=+++5", 5);
    assertUnaryAssignConstant("a=--5", 5);
    assertUnaryAssignConstant("a=-+-5", 5);
    assertUnaryAssignConstant("a=--+5", 5);
  }

  private void assertUnaryAssignConstant(String expression, int value) {
    Lexer lexer = new Lexer(expression);
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    assertThat(root.children()).hasSize(1);

    AssignmentNode node = (AssignmentNode) root.children().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.INT);
    assertThat(((IntNode) expr).value()).isEqualTo(value);
  }

  @Test
  public void testParse_program() {
    Lexer lexer = new Lexer("a=3 print a\n abc =   123 +a-b print 123\nprin=t");
    Parser parser = new Parser(lexer);

    Node rootNode = parser.parse();
    assertWithMessage(rootNode.toString()).that(rootNode.isError()).isFalse();

    StatementsNode root = (StatementsNode) rootNode;
//    System.out.println(root);

    List<StatementNode> children = root.children();
    assertThat(children).hasSize(5);

    assertThat(children.get(0).nodeType()).isEqualTo(Node.Type.ASSIGNMENT);
    assertThat(children.get(1).nodeType()).isEqualTo(Node.Type.PRINT);
    assertThat(children.get(2).nodeType()).isEqualTo(Node.Type.ASSIGNMENT);
    assertThat(children.get(3).nodeType()).isEqualTo(Node.Type.PRINT);
    assertThat(children.get(4).nodeType()).isEqualTo(Node.Type.ASSIGNMENT);
  }

  @Test
  public void testParse_assignmentParens() {
    Lexer lexer = new Lexer("a=(3)");
    Parser parser = new Parser(lexer);

    Node rootNode = parser.parse();
    assertWithMessage(rootNode.toString()).that(rootNode.isError()).isFalse();
    StatementsNode root = (StatementsNode) rootNode;

    assertThat(root.children()).hasSize(1);
    AssignmentNode node = (AssignmentNode) root.children().get(0);
    assertThat(node.nodeType()).isEqualTo(Node.Type.ASSIGNMENT);

    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.INT);
    IntNode intNode = (IntNode) expr;
    assertThat(intNode.value()).isEqualTo(3);
  }

}
