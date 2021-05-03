package com.plasstech.lang.d2.parse;

import static com.google.common.truth.Truth.assertThat;
// import static com.google.common.truth.Truth.assertThat;
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

    Node expr = node.expr();
    assertThat(expr.nodeType()).isEqualTo(Node.Type.INT);
    IntNode intNode = (IntNode) expr;
    assertThat(intNode.value()).isEqualTo(123);
  }

  @Test
  public void testParse_printErr() {
    Lexer lexer = new Lexer("print");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
  }

  @Test
  public void testParse_assignErr() {
    Lexer lexer = new Lexer("a=");
    Parser parser = new Parser(lexer);

    Node node = parser.parse();
    assertThat(node.nodeType()).isEqualTo(Node.Type.ERROR);
  }

  @Test
  public void testParse_assignmentConst() {
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
    assertThat(binOp.opType()).isEqualTo(Token.Type.PLUS);
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
    assertThat(binOp.opType()).isEqualTo(Token.Type.MULT);
    IntNode right = (IntNode) binOp.right();
    assertThat(right.value()).isEqualTo(4);
  }

  @Test
  public void testParse_assignmentAddChained() {
    Lexer lexer = new Lexer("a=3+4*b-5");
    Parser parser = new Parser(lexer);

    Node rootNode = parser.parse();
    assertWithMessage(rootNode.toString()).that(rootNode.isError()).isFalse();
    System.out.println(rootNode);
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
  public void testParse_program() {
    Lexer lexer = new Lexer("a=3 print a\n abc =   123 +a-b print 123\nprin=t");
    Parser parser = new Parser(lexer);

    Node rootNode = parser.parse();
    assertWithMessage(rootNode.toString()).that(rootNode.isError()).isFalse();

    StatementsNode root = (StatementsNode) rootNode;
//    System.out.println("Program:");
//    System.out.println(root);
//    System.out.println();

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
