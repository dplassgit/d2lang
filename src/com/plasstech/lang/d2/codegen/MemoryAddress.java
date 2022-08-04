package com.plasstech.lang.d2.codegen;

import java.util.Objects;

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
  public boolean equals(Object that) {
    if (that == null || !(that instanceof MemoryAddress)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass().getName(), name(), type(), storage());
  }
}
