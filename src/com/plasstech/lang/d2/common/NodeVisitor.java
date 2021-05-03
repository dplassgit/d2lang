package com.plasstech.lang.d2.common;

import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.VariableNode;

public interface NodeVisitor {

  void accept(PrintNode printNode);

  void accept(AssignmentNode node);

  void accept(BinOpNode binOpNode);

  void accept(IntNode intNode);

  void accept(VariableNode variableNode);
}
