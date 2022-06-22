package com.plasstech.lang.d2.type;

public interface Symbol {

  Symbol setVarType(VarType varType);

  VarType varType();

  Symbol setAssigned();

  boolean isAssigned();

  // this is insufficient - if it's register or stack, we need to know *where* it is
  SymbolStorage storage();
}
