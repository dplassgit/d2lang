package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

public class StringNode extends SimpleNode {
  private final String value;

  public StringNode(String value, Position position) {
    super(Type.STRING, position);
    this.value = value;
    setVarType(VarType.STRING);
  }

  public String value() {
    return value;
  }

  @Override
  public String simpleValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.format("StringNode: %s", value);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
