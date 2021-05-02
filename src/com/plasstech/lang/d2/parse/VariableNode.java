package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;

public class VariableNode extends AtomNode {
  // TODO: augment with type of variable.
  private final String name;

  public VariableNode(String name) {
    super(Type.VARIABLE);
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return String.format("VarNode: %s", name);
  }

  @Override
  public void visit(NodeVisitor visitor) {
    visitor.accept(this);
  }
}
