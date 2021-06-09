package com.plasstech.lang.d2.type;

public class ArrayType implements VarType {
  private final VarType baseType;

  public ArrayType(VarType baseType) {
    this.baseType = baseType;
  }

  @Override
  public String name() {
    return String.format("array:%s", baseType.name());
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
}
