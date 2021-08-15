package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class MemoryAddress extends Location {
  // TODO: capture memory location
  public MemoryAddress(String name, VarType varType) {
    super(name, varType);
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.GLOBAL;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof MemoryAddress)) {
      return false;
    }
    Location that = (Location) obj;
    return this.storage() == that.storage() && this.name().equals(that.name());
  }

  @Override
  public int hashCode() {
    return 47 + 7 * this.name().hashCode();
  }
}
