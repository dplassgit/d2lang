package com.plasstech.lang.d2.parse.node;

public class DefaultNodeVisitor implements NodeVisitor {

  @Override
  public void visit(ProgramNode node) {
    // default implementation
    if (node != null) {
      node.statements().accept(this);
    }
  }

  @Override
  public void visit(IfNode node) {
    for (IfNode.Case ifCase : node.cases()) {
      ifCase.block().accept(this);
    }
    node.elseBlock().ifPresent(block -> block.accept(this));
  }

  @Override
  public void visit(WhileNode node) {
    node.block().accept(this);
  }

  @Override
  public void visit(ProcedureNode node) {
    node.block().accept(this);
  }

  @Override
  public void visit(BlockNode node) {
    node.statements().forEach((s) -> s.accept(this));
  }

  @Override
  public void visit(PrintNode node) {}

  @Override
  public void visit(AssignmentNode node) {}

  @Override
  public void visit(BinOpNode node) {}

  @Override
  public void visit(VariableNode node) {}

  @Override
  public <T> void visit(ConstNode<T> node) {}

  @Override
  public void visit(UnaryNode node) {}

  @Override
  public void visit(ExternProcedureNode node) {}

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

  @Override
  public void visit(InputNode node) {}

  @Override
  public void visit(NewNode node) {}

  @Override
  public void visit(RecordDeclarationNode node) {}

  @Override
  public void visit(ArrayLiteralNode node) {}

  @Override
  public void visit(IncDecNode node) {}
}
