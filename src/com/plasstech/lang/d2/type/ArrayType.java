package com.plasstech.lang.d2.type;

import java.util.Objects;

public class ArrayType implements VarType {
  private final VarType baseType;
  private final int dimensions;

  public ArrayType(VarType baseType /*, int dimensions*/) {
    this.baseType = baseType;
    this.dimensions = 1;
  }

  @Override
  public String name() {
    return "ARRAY";
  }

  @Override
  public int size() {
    return 8;
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
    return String.format("%d-d ARRAY of %s", dimensions(), baseType.name());
  }
}
