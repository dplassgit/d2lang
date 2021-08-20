package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class RegisterLocation extends Location {

  private final Register register;

  // variable name, register (rax->r15)
  public RegisterLocation(String varName, Register register, VarType varType) {
    super(varName, varType);
    this.register = register;
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.REGISTER;
  }

  public Register register() {
    return register;
  }
}
