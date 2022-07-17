package com.plasstech.lang.d2.type;

public interface Symbol {

  Symbol setVarType(VarType varType);

  VarType varType();

  Symbol setAssigned();

  boolean isAssigned();

  /**
   * Locals or parameters will necessarily require more data than just "storage type" - they need an
   * index or stack offset.
   */
  SymbolStorage storage();

  default boolean isVariable() {
    return false;
  }
}
