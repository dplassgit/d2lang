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
}
