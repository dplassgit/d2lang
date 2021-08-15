package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class TempLocation extends Location {
  public TempLocation(String name, VarType varType) {
    super(name, varType);
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
