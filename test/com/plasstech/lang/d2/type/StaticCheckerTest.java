package com.plasstech.lang.d2.type;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.AssignmentNode;
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
    checker.execute();
  }

  @Test
  public void execute_assign() {
    Lexer lexer = new Lexer("a=3");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    checker.execute();

    AssignmentNode node = (AssignmentNode) root.children().get(0);
    VariableNode var = node.variable();
    assertThat(var.name()).isEqualTo("a");
    assertThat(var.varType()).isEqualTo(VarType.INT);

    Node expr = node.expr();
    IntNode intNode = (IntNode) expr;
    assertThat(intNode.varType()).isEqualTo(VarType.INT);
  }

//  @Test
//  public void execute_assignMulti() {
//    Lexer lexer = new Lexer("a=3 b=a");
//    Parser parser = new Parser(lexer);
//
//    StatementsNode root = (StatementsNode) parser.parse();
//    StaticChecker checker = new StaticChecker(root);
//    checker.execute();
//
//    AssignmentNode node = (AssignmentNode) root.children().get(1);
//    VariableNode var = node.variable();
//    assertThat(var.name()).isEqualTo("b");
//    assertThat(var.varType()).isEqualTo(VarType.INT);
//
//    Node expr = node.expr();
//    VariableNode rhsNode = (VariableNode) expr;
//    assertThat(rhsNode.varType()).isEqualTo(VarType.INT);
//  }
}
