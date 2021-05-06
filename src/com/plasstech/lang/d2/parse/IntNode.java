package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/**
 * Represents a constant integer.
 */
public class IntNode extends SimpleNode {
  private final int value;

  public IntNode(int value, Position position) {
    super(Type.INT, position);
    this.value = value;
    setVarType(VarType.INT);
  }

  public int value() {
    return value;
  }

  @Override
  public String simpleValue() {
    return String.valueOf(value);
  }

  @Override
  public String toString() {
    return String.format("IntNode: %d", value);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
