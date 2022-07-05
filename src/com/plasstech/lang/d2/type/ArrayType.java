package com.plasstech.lang.d2.type;

public class ArrayType implements VarType {
  private final VarType baseType;
  private final int dimensions;

  public ArrayType(VarType baseType /*, int dimensions*/) {
    this.baseType = baseType;
    this.dimensions = 1;
  }

  @Override
  public String name() {
    return String.format("array of %s", baseType.name());
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
    if (!(thatType instanceof ArrayType)) {
      return false;
    }
    ArrayType that = (ArrayType) thatType;
    /** also compare dimensions */
    return this.baseType().compatibleWith(that.baseType());
  }

  @Override
  public boolean equals(Object thatObject) {
    if (thatObject == null || !(thatObject instanceof ArrayType)) {
      return false;
    }
    ArrayType that = (ArrayType) thatObject;
    return this.baseType.equals(that.baseType);
  }
  
  @Override
  public int hashCode() {
    return 23 * name().hashCode() + 7;
  }

  @Override
  public String toString() {
    return name();
  }
}
