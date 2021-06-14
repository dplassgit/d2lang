package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;

/** Represents a variable access, or a variable assignment. */
public class VariableNode extends AbstractNode implements SimpleNode {
  private final String name;

  public VariableNode(String name, Position position) {
    super(position);
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public String simpleValue() {
    return String.valueOf(name);
  }

  @Override
  public String toString() {
    return String.format("VarNode: %s (%s)", name, varType());
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
