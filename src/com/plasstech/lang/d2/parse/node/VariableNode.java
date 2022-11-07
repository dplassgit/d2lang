package com.plasstech.lang.d2.parse.node;

import com.google.common.base.Objects;
import com.plasstech.lang.d2.common.Position;

/** Represents a variable access, or a variable assignment. */
public class VariableNode extends AbstractNode implements ExprNode {
  private final String name;

  public VariableNode(String name, Position position) {
    super(position);
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof VariableNode)) {
      return false;
    }
    return this.hashCode() == obj.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name, varType(), getClass());
  }
}
