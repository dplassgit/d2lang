package com.plasstech.lang.d2.type;

public interface Symbol {

  Symbol setVarType(VarType varType);

  VarType varType();

  Symbol setAssigned();

  boolean isAssigned();

  SymbolStorage storage();
}
