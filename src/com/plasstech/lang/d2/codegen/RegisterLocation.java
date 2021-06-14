package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;

public class RegisterLocation extends Location {

  // TODO: capture register !
  public RegisterLocation(String name) {
    super(name);
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.REGISTER;
  }
}
