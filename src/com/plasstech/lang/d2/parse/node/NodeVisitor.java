package com.plasstech.lang.d2.parse.node;

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
