package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import org.junit.Test;

import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.StatementsNode;
import com.plasstech.lang.d2.parse.VariableNode;

public class StaticCheckerTest {

  @Test
  public void execute_print() {
    Lexer lexer = new Lexer("print 123");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertThat(result.isError()).isFalse();
  }

  @Test
  public void execute_assignInt() {
    Lexer lexer = new Lexer("a=3");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertThat(result.isError()).isFalse();
    SymTab types = result.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.children().get(0);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    IntNode intNode = (IntNode) expr;
    assertThat(intNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_assignBool() {
    Lexer lexer = new Lexer("a=true");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertThat(result.isError()).isFalse();
    SymTab types = result.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);

    AssignmentNode node = (AssignmentNode) root.children().get(0);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.BOOL);

    Node expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void execute_assignExpr() {
    Lexer lexer = new Lexer("a=3+4-9");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertThat(result.isError()).isFalse();
    SymTab types = result.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.children().get(0);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    BinOpNode binOpNode = (BinOpNode) expr;
    assertThat(binOpNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_assignExprIndeterminable() {
    Lexer lexer = new Lexer("a=3+(4-b)");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertThat(result.isError()).isTrue();
    System.err.println(result.message());
    assertThat(result.message()).contains("Indeterminable type");
  }

  @Test
  public void execute_assignExprIndeterminableMultiple() {
    Lexer lexer = new Lexer("a=3 b=(((a+3))) c=d");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertThat(result.isError()).isTrue();
    System.err.println(result.message());
    assertThat(result.message()).contains("Indeterminable type");
  }

  @Test
  public void execute_assignMulti() {
    Lexer lexer = new Lexer("a=3 b=a c = b+4 print c");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertThat(result.isError()).isFalse();
    SymTab types = result.symbolTable();

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of c").that(types.lookup("c")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.children().get(1);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("b");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    VariableNode rhsNode = (VariableNode) expr;
    assertThat(rhsNode.varType()).isEqualTo(VarType.INT);
  }

  @Test
  public void execute_assignMismatch() {
    Lexer lexer = new Lexer("a=true b=3 b=a");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertThat(result.isError()).isTrue();
    System.err.println(result.message());
    assertThat(result.message()).contains("Type mismatch");
  }

  @Test
  public void execute_binOpMismatch() {
    Lexer lexer = new Lexer("a=true+3");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    assertThat(result.isError()).isTrue();
    System.err.println(result.message());
    assertThat(result.message()).contains("Type mismatch");
  }
}
