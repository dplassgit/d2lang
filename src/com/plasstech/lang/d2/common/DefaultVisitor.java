package com.plasstech.lang.d2.common;

import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.BoolNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.VariableNode;

public class DefaultVisitor implements NodeVisitor {

  @Override
  public void visit(PrintNode node) {
  }

  @Override
  public void visit(AssignmentNode node) {
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
}
