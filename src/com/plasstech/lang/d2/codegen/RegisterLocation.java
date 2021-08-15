package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class RegisterLocation extends Location {

  // TODO: capture WHICH register?
  public RegisterLocation(String varName, String regName, VarType varType) {
    super(varName, varType);
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.REGISTER;
  }
}
