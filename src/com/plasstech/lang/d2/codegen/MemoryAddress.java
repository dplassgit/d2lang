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

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof MemoryAddress)) {
      return false;
    }
    return this.name().equals(((MemoryAddress) that).name());
  }

  @Override
  public int hashCode() {
    return 47 + 7 * this.name().hashCode();
  }
}
