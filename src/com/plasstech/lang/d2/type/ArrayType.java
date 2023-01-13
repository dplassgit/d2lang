package com.plasstech.lang.d2.type;

import java.util.Objects;

public class ArrayType extends PointerType {
  private final VarType baseType;
  private final int dimensions;

  public ArrayType(VarType baseType, int dimensions) {
    super("ARRAY of " + baseType.name());
    this.baseType = baseType;
    this.dimensions = dimensions;
  }

  public int dimensions() {
    return dimensions;
  }

  @Override
  public boolean isArray() {
    return true;
  }

  public VarType baseType() {
    return baseType;
  }

  @Override
  public boolean compatibleWith(VarType thatType) {
    return this.equals(thatType);
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof ArrayType)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name(), dimensions(), baseType());
  }

  @Override
  public String toString() {
    return String.format("%d-d %s", dimensions(), name());
  }
}
