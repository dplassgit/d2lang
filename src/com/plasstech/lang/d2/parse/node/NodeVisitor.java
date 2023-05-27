package com.plasstech.lang.d2.parse.node;

public interface NodeVisitor {

  void visit(PrintNode node);

  void visit(AssignmentNode node);

  void visit(ArrayLiteralNode node);

  void visit(BinOpNode node);

  <T> void visit(ConstNode<T> node);

  void visit(VariableNode node);

  void visit(UnaryNode node);

  void visit(IfNode node);

  void visit(ProcedureNode node);

  void visit(WhileNode node);

  void visit(BreakNode node);

  void visit(ContinueNode node);

  void visit(DeclarationNode node);

  void visit(RecordDeclarationNode node);

  void visit(CallNode node);

  void visit(ReturnNode node);

  void visit(ExitNode node);

  void visit(ArrayDeclarationNode node);

  void visit(InputNode node);

  void visit(NewNode node);

  void visit(ExternProcedureNode node);

  void visit(IncDecNode incDecNode);
}
