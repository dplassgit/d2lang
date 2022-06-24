package com.plasstech.lang.d2.type;

/** A simple type representing NULL - compatible with any record type, and nulls. */
public class NullType extends SimpleType {
  public NullType() {
    super("NULL");
  }

  @Override
  public boolean isNull() {
    return true;
  }

  @Override
  public boolean compatibleWith(VarType that) {
    return that.isRecord() || that.isNull();
  }
}
