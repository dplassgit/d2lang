package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;

public class TempLocation extends Location {
  public TempLocation(String name) {
    super(name);
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.TEMP;
  }
}
