package com.plasstech.lang.d2.type;

class StringType extends PointerType {
  StringType() {
    super("STRING");
  }

  @Override
  public final boolean compatibleWith(VarType that) {
    return super.compatibleWith(that) || that.isNull();
  }
}
