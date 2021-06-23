package com.plasstech.lang.d2.parse.node;

public class DefaultVisitor implements NodeVisitor {

  @Override
  public void visit(PrintNode node) {}

  @Override
  public void visit(AssignmentNode node) {}

  @Override
  public void visit(IfNode ifNode) {}

  @Override
  public void visit(BinOpNode node) {}

  @Override
  public void visit(VariableNode node) {}

  @Override
  public <T> void visit(ConstNode<T> node) {}

  @Override
  public void visit(UnaryNode unaryNode) {}

  @Override
  public void visit(ProcedureNode node) {}

  @Override
  public void visit(MainNode node) {}

  @Override
  public void visit(WhileNode node) {}

  @Override
  public void visit(BreakNode node) {}

  @Override
  public void visit(ContinueNode node) {}

  @Override
  public void visit(DeclarationNode node) {}

  @Override
  public void visit(CallNode node) {}

  @Override
  public void visit(ReturnNode node) {}

  @Override
  public void visit(ExitNode node) {}

  @Override
  public void visit(ArrayDeclarationNode node) {}
}
