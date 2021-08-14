package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class RegisterLocation extends Location {

  private final String register;

  public RegisterLocation(String varName, String register, VarType varType) {
    super(varName, varType);
    this.register = register;
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.REGISTER;
  }

  public String register() {
    return register;
  }
}
