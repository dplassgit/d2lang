package com.plasstech.lang.d2.type;

/** A simple type representing NULL - compatible with any record type, and nulls. */
class NullType extends PointerType {
  NullType() {
    super("NULL");
  }

  @Override
  public boolean compatibleWith(VarType that) {
    return that == VarType.STRING || that.isRecord() || that.isArray() || that.isNull();
  }
}
