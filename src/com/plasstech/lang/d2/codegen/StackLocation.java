package com.plasstech.lang.d2.codegen;

import java.util.Objects;

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

  /** this is ALWAYS positive */
  public int offset() {
    return offset;
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof StackLocation)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass().getName(), name(), type(), offset(), storage());
  }
}
