package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;

public class MemoryAddress extends Location {
  // TODO: capture memory location
  public MemoryAddress(String name) {
    super(name);
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.GLOBAL;
  }
}
