package com.plasstech.lang.d2.type;

abstract class DefaultVarType implements VarType {

  private final String name;

  protected DefaultVarType(String name) {
    this.name = name;
    VarType.register(this, name);
  }

  @Override
  final public String name() {
    return name;
  }
}
