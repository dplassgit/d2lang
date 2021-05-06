package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

public class BoolNode extends SimpleNode {
  private final boolean value;

  public BoolNode(boolean value, Position position) {
    super(Type.BOOL, position);
    this.value = value;
    setVarType(VarType.BOOL);
  }

  public boolean value() {
    return value;
  }

  @Override
  public String simpleValue() {
    return String.valueOf(value);
  }

  @Override
  public String toString() {
    return String.format("BoolNode: %s", value);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
