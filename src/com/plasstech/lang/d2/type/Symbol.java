package com.plasstech.lang.d2.type;

public class Symbol {
  private final String name;
  private final SymbolStorage storage;
  private boolean assigned;
  private VarType type;

  // future: location (memory, stack, register)
  // maybe future: scope (sym tab?)

  public Symbol(String name, SymbolStorage storage) {
    this.name = name;
    this.storage = storage;
  }

  public String name() {
    return name;
  }

  public boolean isAssigned() {
    return assigned;
  }

  public Symbol setAssigned() {
    this.assigned = true;
    return this;
  }

  public VarType type() {
    return type;
  }

  public Symbol setType(VarType type) {
    this.type = type;
    return this;
  }

  public SymbolStorage storage() {
    return storage;
  }

  @Override
  public int hashCode() {
    return name.toLowerCase().hashCode();
  }
}
