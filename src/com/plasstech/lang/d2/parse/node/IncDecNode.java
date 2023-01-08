package com.plasstech.lang.d2.parse.node;

/** Represents an increment or decrement statement, which is syntactic sugar for i=i=1 or i=i-1. */
public class IncDecNode extends AbstractNode implements StatementNode {

  private final boolean increment;
  private final VariableNode variable;

  public IncDecNode(VariableNode variable, boolean increment) {
    super(variable.position());
    this.increment = increment;
    this.variable = variable;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  public String name() {
    return variable.name();
  }

  public boolean isIncrement() {
    return increment;
  }
}
