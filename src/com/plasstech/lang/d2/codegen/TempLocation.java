package com.plasstech.lang.d2.codegen;

import java.util.Objects;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VariableSymbol;

// Can be a register or on the stack
public class TempLocation extends VariableLocation {
  public TempLocation(VariableSymbol symbol) {
    super(symbol);
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.TEMP;
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof TempLocation)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass().getName(), name(), type());
  }
}
