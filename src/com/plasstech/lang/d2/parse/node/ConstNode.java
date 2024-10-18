package com.plasstech.lang.d2.parse.node;

import com.google.common.base.Objects;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/** Represents an int, boolean, string or float constant node. */
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
    } else if (value == null) {
      return "(null)";
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
    if (value == null) {
      return Objects.hashCode(varType(), getClass());
    }
    return Objects.hashCode(value, value.getClass(), varType(), getClass());
  }

  public Number valueAsNumber() {
    if (!(value instanceof Number)) {
      throw new IllegalArgumentException(
          "Cannot get Number const from non-numeric ConstNode: " + this);
    }
    return (Number) value();
  }

  public static ConstNode<? extends Number> fromValue(long value, VarType type, Position position) {
    if (type == VarType.LONG) {
      return new ConstNode<Long>(value, type, position);
    }
    if (type == VarType.INT) {
      return new ConstNode<Integer>((int) value, type, position);
    }
    if (type == VarType.BYTE) {
      return new ConstNode<Byte>((byte) value, type, position);
    }
    throw new IllegalStateException("Cannot take fromValue of type " + type);
  }
}
