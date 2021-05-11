package com.plasstech.lang.d2.common;

import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.BoolNode;
import com.plasstech.lang.d2.parse.BreakNode;
import com.plasstech.lang.d2.parse.ContinueNode;
import com.plasstech.lang.d2.parse.DeclarationNode;
import com.plasstech.lang.d2.parse.IfNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.MainNode;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.ProcedureNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;
import com.plasstech.lang.d2.parse.WhileNode;

public class DefaultVisitor implements NodeVisitor {

  @Override
  public void visit(PrintNode node) {
  }

  @Override
  public void visit(AssignmentNode node) {
  }

  @Override
  public void visit(IfNode ifNode) {
  }

  @Override
  public void visit(BinOpNode node) {
  }

  @Override
  public void visit(IntNode node) {
  }

  @Override
  public void visit(VariableNode node) {
  }

  @Override
  public void visit(BoolNode boolNode) {
  }

  @Override
  public void visit(UnaryNode unaryNode) {
  }

  @Override
  public void visit(ProcedureNode node) {
  }

  @Override
  public void visit(MainNode node) {
  }

  @Override
  public void visit(WhileNode node) {
  }

  @Override
  public void visit(BreakNode node) {
  }

  @Override
  public void visit(ContinueNode node) {
  }

  @Override
  public void visit(DeclarationNode node) {
  }
}
