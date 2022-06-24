package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class StackLocation extends Location {

  private final int offset;

  public StackLocation(String name, VarType varType, int offset) {
    super(name, varType);
    this.offset = offset;
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.LOCAL;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof StackLocation)) {
      return false;
    }
    Location that = (Location) obj;
    return this.storage() == that.storage() && this.name().equals(that.name());
  }

  @Override
  public int hashCode() {
    return 37 + 11 * this.name().hashCode() + 5 * offset();
  }

  /** this is ALWAYS positive */
  public int offset() {
    return offset;
  }
}
