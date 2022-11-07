package com.plasstech.lang.d2.parse.node;

import com.google.common.base.Objects;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/** Represents an int, boolean, string or (someday) float constant node. */
public class ConstNode<T> extends AbstractNode implements ExprNode {

  private final T value;

  public ConstNode(T value, VarType varType, Position position) {
    super(position);
    this.value = value;
    setVarType(varType);
  }

  @Override
  public boolean isConstant() {
    return true;
  }

  public T value() {
    return value;
  }

  @Override
  public String toString() {
    if (varType() == VarType.STRING) {
      return String.format("'%s'", value);
    } else {
      return value.toString();
    }
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof ConstNode)) {
      return false;
    }
    return this.hashCode() == obj.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(value, value.getClass(), varType(), getClass());
  }
}
