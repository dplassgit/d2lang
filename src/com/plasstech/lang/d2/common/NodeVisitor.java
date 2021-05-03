package com.plasstech.lang.d2.common;

import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.VariableNode;

public interface NodeVisitor {

  void visit(PrintNode printNode);

  void visit(AssignmentNode node);

  void visit(BinOpNode binOpNode);

  void visit(IntNode intNode);

  void visit(VariableNode variableNode);
}
