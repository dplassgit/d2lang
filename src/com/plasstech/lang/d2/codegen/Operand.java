package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

/** Represents an operand in a binary or unary operation. Either a location or a constant. */
public interface Operand {
  SymbolStorage storage();

  VarType type();

  // I'm not loving these methods.
  default boolean isConstant() {
    return false;
  }

  default boolean isRegister() {
    return false;
  }

  default boolean isTemp() {
    return this.storage() == SymbolStorage.TEMP;
  }
}
