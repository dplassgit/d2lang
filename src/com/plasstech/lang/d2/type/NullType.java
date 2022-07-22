package com.plasstech.lang.d2.type;

/** A simple type representing NULL - compatible with any record type, and nulls. */
public class NullType extends SimpleType {
  public NullType() {
    // nulls are pointers so their size is 8 (!)
    super("NULL", 8);
  }
  
  @Override
  public boolean isNull() {
    return true;
  }

  @Override
  public boolean compatibleWith(VarType that) {
    return that == VarType.STRING || that.isRecord() || that.isNull();
  }
}
