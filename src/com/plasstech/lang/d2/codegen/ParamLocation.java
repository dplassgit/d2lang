package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class ParamLocation extends Location {
  private final int index;

  public ParamLocation(String varName, VarType varType, int index) {
    super(varName, varType);
    this.index = index;
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.PARAM;
  }

  public int index() {
    return index;
  }

  @Override
  public boolean equals(Object obj) {
    if (!(obj instanceof ParamLocation)) {
      return false;
    }
    ParamLocation that = (ParamLocation) obj;

    return this.name().equals(that.name())
        && this.type().equals(that.type())
        && this.index == that.index;
  }

  @Override
  public int hashCode() {
    return name().hashCode() * 7 + type().hashCode() * 13 + index * 41;
  }
}
