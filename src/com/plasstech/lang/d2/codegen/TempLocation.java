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

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof TempLocation)) {
      return false;
    }
    return this.name().equals(((TempLocation) that).name());
  }

  @Override
  public int hashCode() {
    return 41 + 13 * this.name().hashCode();
  }
}
