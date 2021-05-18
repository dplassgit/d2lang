package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/**
 * Represents an int, boolean, string or (someday) float node.
 */
public class ConstNode<T> extends SimpleNode {

  private final T value;

  ConstNode(T value, VarType varType, Position position) {
    super(position);
    this.value = value;
    setVarType(varType);
  }

  @Override
  public boolean isSimpleType() {
    return true;
  }

  public T value() {
    return value;
  }

  @Override
  public String simpleValue() {
    return String.valueOf(value);
  }

  @Override
  public String toString() {
    return String.format("ConstNode(%s): %s", varType().name().toLowerCase(), value);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
