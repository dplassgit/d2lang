package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;

public class IntNode extends AtomNode {
  private final int value;

  public IntNode(int value) {
    super(Type.INT);
    this.value = value;
  }

  public int value() {
    return value;
  }

  @Override
  public String toString() {
    return String.format("IntNode: %d", value);
  }

  @Override
  public void visit(NodeVisitor visitor) {
    visitor.accept(this);
  }
}
