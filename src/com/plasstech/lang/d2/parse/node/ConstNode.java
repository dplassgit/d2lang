package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/**
 * Represents an int, boolean, string or (someday) float constant node.
 */
public class ConstNode<T> extends AbstractNode implements SimpleNode {

  private final T value;

  public ConstNode(T value, VarType varType, Position position) {
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
    if (value instanceof String) {
      return String.format("{ConstNode(%s): '%s'}", varType().name().toLowerCase(), value);
    } else {
      return String.format("{ConstNode(%s): %s}", varType().name().toLowerCase(), value);
    }
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
