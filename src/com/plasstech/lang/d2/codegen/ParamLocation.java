package com.plasstech.lang.d2.codegen;

import java.util.Objects;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class ParamLocation extends Location {
  private final int index;
  private final int offset;

  public ParamLocation(String varName, VarType varType, int index, int offset) {
    super(varName, varType);
    this.index = index;
    this.offset = offset;
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.PARAM;
  }

  public int index() {
    return index;
  }

  public int offset() {
    return offset;
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof ParamLocation)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass().getName(), name(), type(), storage(), index, offset);
  }
}
