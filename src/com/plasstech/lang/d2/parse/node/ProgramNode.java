package com.plasstech.lang.d2.parse.node;

public class ProgramNode extends AbstractNode {

  private final BlockNode statements;

  public ProgramNode(BlockNode statements) {
    super(statements.position());
    this.statements = statements;
  }

  public BlockNode statements() {
    return statements;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return statements().toString();
  }
}
