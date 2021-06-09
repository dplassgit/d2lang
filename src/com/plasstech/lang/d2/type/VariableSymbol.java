package com.plasstech.lang.d2.type;

public class VariableSymbol extends AbstractSymbol {

  public VariableSymbol(String name) {
    super(name);
  }

  public SymbolStorage storage() {
    return SymbolStorage.GLOBAL;
  }

  @Override
  public String toString() {
    return String.format("%s: %s", name(), type().toString());
  }
}
