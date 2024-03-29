package com.plasstech.lang.d2.codegen.x64;

import java.util.Objects;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class RegisterLocation extends Location {

  private final Register register;

  // variable name, register (r1->r15)
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

  @Override
  public String name() {
    return register.name();
  }

  @Override
  public boolean isRegister() {
    return true;
  }

  @Override
  public boolean equals(Object that) {
    if (that == null || !(that instanceof RegisterLocation)) {
      return false;
    }
    return this.hashCode() == that.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(getClass().getName(), name(), type(), storage());
  }
}
