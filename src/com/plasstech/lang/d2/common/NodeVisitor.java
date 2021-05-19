package com.plasstech.lang.d2.common;

import com.plasstech.lang.d2.parse.AssignmentNode;
import com.plasstech.lang.d2.parse.BinOpNode;
import com.plasstech.lang.d2.parse.BreakNode;
import com.plasstech.lang.d2.parse.CallNode;
import com.plasstech.lang.d2.parse.ConstNode;
import com.plasstech.lang.d2.parse.ContinueNode;
import com.plasstech.lang.d2.parse.DeclarationNode;
import com.plasstech.lang.d2.parse.IfNode;
import com.plasstech.lang.d2.parse.MainNode;
import com.plasstech.lang.d2.parse.PrintNode;
import com.plasstech.lang.d2.parse.ProcedureNode;
import com.plasstech.lang.d2.parse.ReturnNode;
import com.plasstech.lang.d2.parse.UnaryNode;
import com.plasstech.lang.d2.parse.VariableNode;
import com.plasstech.lang.d2.parse.WhileNode;

public interface NodeVisitor {

  void visit(PrintNode node);

  void visit(AssignmentNode node);

  void visit(BinOpNode node);

  <T> void visit(ConstNode<T> node);

  void visit(VariableNode node);

  void visit(UnaryNode unaryNode);

  void visit(IfNode ifNode);

  void visit(ProcedureNode node);

  void visit(MainNode node);

  void visit(WhileNode node);

  void visit(BreakNode node);

  void visit(ContinueNode node);

  void visit(DeclarationNode node);

  void visit(CallNode callNode);

  void visit(ReturnNode returnNode);
}
