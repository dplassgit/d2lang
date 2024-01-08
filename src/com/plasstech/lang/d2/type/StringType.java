package com.plasstech.lang.d2.type;

class StringType extends PointerType {
  StringType() {
    super("STRING");
  }

  @Override
  final public boolean compatibleWith(VarType that) {
    return super.compatibleWith(that) || that.isNull();
  }
}