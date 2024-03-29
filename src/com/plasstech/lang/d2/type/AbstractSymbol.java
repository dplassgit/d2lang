package com.plasstech.lang.d2.type;

abstract class AbstractSymbol implements Symbol {
  private final String name;
  private boolean assigned;
  private VarType type;

  public AbstractSymbol(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public boolean isAssigned() {
    return assigned;
  }

  @Override
  public Symbol setAssigned() {
    this.assigned = true;
    return this;
  }

  @Override
  public VarType varType() {
    return type;
  }

  @Override
  public Symbol setVarType(VarType type) {
    this.type = type;
    return this;
  }

  @Override
  public int hashCode() {
    return name.toLowerCase().hashCode();
  }
}
