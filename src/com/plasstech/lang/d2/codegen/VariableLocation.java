package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VariableSymbol;

public abstract class VariableLocation extends Location {
  private final VariableSymbol symbol;

  public VariableLocation(VariableSymbol symbol) {
    super(symbol.name(), symbol.varType());
    this.symbol = symbol;
  }

  @Override
  public SymbolStorage storage() {
    return symbol().storage();
  }

  public VariableSymbol symbol() {
    return symbol;
  }
}