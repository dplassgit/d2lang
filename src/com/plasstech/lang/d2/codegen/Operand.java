package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

/** Represents an operand in a binary or unary operation. Either a location or a constant. */
public interface Operand {
  SymbolStorage storage();

  VarType type();

  default boolean isConstant() {
    return false;
  }
}
