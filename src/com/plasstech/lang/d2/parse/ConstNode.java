package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/**
 * Represents an int, boolean, string or (someday) float node.
 */
public class ConstNode<T> extends SimpleNode {

  private final T value;

  ConstNode(Type type, T value, VarType varType, Position position) {
    super(type, position);
    this.value = value;
    setVarType(varType);
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
    return String.format("ConstNode(%s): %s", nodeType().name().toLowerCase(), value);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
