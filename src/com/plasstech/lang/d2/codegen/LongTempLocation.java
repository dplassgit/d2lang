package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VariableSymbol;

public class LongTempLocation extends TempLocation {
  public LongTempLocation(VariableSymbol symbol) {
    super(symbol);
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.LONG_TEMP;
  }
}
