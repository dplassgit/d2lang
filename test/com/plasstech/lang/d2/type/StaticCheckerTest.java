package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.StatementsNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;

public class StaticCheckerTest {

  @Test
  public void execute_print() {
    Lexer lexer = new Lexer("print 123");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    execute(checker);
  }

  @Test
  public void execute_assignInt() {
    Lexer lexer = new Lexer("a=3");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

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
  public void execute_assignUnaryIntConst() {
    Lexer lexer = new Lexer("a=-3");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

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
  public void execute_assignUnaryVar() {
    Lexer lexer = new Lexer("a=3 b=-a");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.children().get(1);
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

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);

    AssignmentNode node = (AssignmentNode) root.children().get(1);
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

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);

    AssignmentNode node = (AssignmentNode) root.children().get(0);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.BOOL);

    Node expr = node.expr();
    assertThat(expr.varType()).isEqualTo(VarType.BOOL);
  }

  @Test
  public void execute_assignBoolConstantUnaryFailure() {
    Lexer lexer = new Lexer("a=-true");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    assertExecuteError(checker, "MINUS");
  }

  @Test
  public void execute_assignBoolUnaryFailure() {
    Lexer lexer = new Lexer("a=true\nb=-a");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    assertExecuteError(checker, "MINUS");
  }

  @Test
  public void execute_assignIntUnaryFailure() {
    Lexer lexer = new Lexer("a=3\nb=!a");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    assertExecuteError(checker, "NOT");
  }

  @Test
  public void execute_assignBoolConstantUnary() {
    Lexer lexer = new Lexer("a=!true");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void execute_assignBoolUnary() {
    Lexer lexer = new Lexer("a=true\nb=!a");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);
    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.BOOL);
  }

  @Test
  public void execute_assignExpr() {
    Lexer lexer = new Lexer("a=3+4-9");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

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
    assertExecuteError(checker, "Indeterminable type");
  }

  @Test
  public void execute_assignExprIndeterminableMultiple() {
    Lexer lexer = new Lexer("a=3 b=a+3 c=d");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    assertExecuteError(checker, "Indeterminable type");
  }

  @Test
  public void execute_assignMulti() {
    Lexer lexer = new Lexer("a=3 b=a c = b+4 d=b==c e=3<4 f=d==true print c");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    SymTab types = execute(checker);

    assertWithMessage("type of a").that(types.lookup("a")).isEqualTo(VarType.INT);
    assertWithMessage("type of b").that(types.lookup("b")).isEqualTo(VarType.INT);
    assertWithMessage("type of c").that(types.lookup("c")).isEqualTo(VarType.INT);
    assertWithMessage("type of e").that(types.lookup("e")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of f").that(types.lookup("f")).isEqualTo(VarType.BOOL);
    assertWithMessage("type of d").that(types.lookup("d")).isEqualTo(VarType.BOOL);

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
    assertExecuteError(checker, "Type mismatch");
  }

  @Test
  public void execute_binOpMismatch() {
    for (String op : ImmutableList.of("==", "!=", "<=", ">=")) {
      Lexer lexer = new Lexer(String.format("a=true%S3", op));
      Parser parser = new Parser(lexer);

      StatementsNode root = (StatementsNode) parser.parse();
      StaticChecker checker = new StaticChecker(root);
      assertExecuteError(checker, "Type mismatch");
    }
  }

  @Test
  public void execute_binOpSingleCharMismatch() {
    for (char c : "+-<>|&".toCharArray()) {
      Lexer lexer = new Lexer(String.format("a=true%c3", c));
      Parser parser = new Parser(lexer);

      StatementsNode root = (StatementsNode) parser.parse();
      StaticChecker checker = new StaticChecker(root);
      assertExecuteError(checker, "Type mismatch");
    }
  }

  private void assertExecuteError(StaticChecker checker, String messageShouldContain) {
    TypeCheckResult result = checker.execute();
    assertThat(result.isError()).isTrue();
    System.err.println(result.message());
    assertThat(result.message()).contains(messageShouldContain);
  }

  private SymTab execute(StaticChecker checker) {
    TypeCheckResult result = checker.execute();
    if (result.isError()) {
      fail(result.message());
    }
    SymTab types = result.symbolTable();
    return types;
  }
}
