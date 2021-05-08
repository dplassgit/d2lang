package com.plasstech.lang.d2.common;

import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.BoolNode;
import com.plasstech.lang.d2.parse.IfNode;
import com.plasstech.lang.d2.parse.IntNode;
import com.plasstech.lang.d2.parse.MainNode;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.ProcedureNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;

public interface NodeVisitor {

  void visit(PrintNode node);

  void visit(AssignmentNode node);

  void visit(BinOpNode node);

  void visit(IntNode node);

  void visit(VariableNode node);

  void visit(BoolNode boolNode);

  void visit(UnaryNode unaryNode);

  void visit(IfNode ifNode);

  void visit(ProcedureNode node);

  void visit(MainNode node);
}
