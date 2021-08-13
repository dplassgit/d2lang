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
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof TempLocation)) {
      return false;
    }
    Location that = (Location) obj;
    return this.storage() == that.storage() && this.name().equals(that.name());
  }

  @Override
  public int hashCode() {
    return 41 + 13 * this.name().hashCode();
  }
}
