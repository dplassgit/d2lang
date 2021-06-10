package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.parse.node.ExprNode;

public class ArrayType implements VarType {
  private final VarType baseType;
  private final ExprNode arraySize;

  public ArrayType(VarType baseType, ExprNode arraySize) {
    this.baseType = baseType;
    this.arraySize = arraySize;
  }

  @Override
  public String name() {
    return String.format("array:%s", baseType.name());
  }

  public VarType baseType() {
    return baseType;
  }

  public ExprNode arraySizeExpr() {
    return arraySize;
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
