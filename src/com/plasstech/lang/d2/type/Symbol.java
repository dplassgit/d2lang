package com.plasstech.lang.d2.type;

public interface Symbol {

  Symbol setType(VarType varType);

  Symbol setAssigned();

  VarType type();

  boolean isAssigned();

  String name();

  SymbolStorage storage();
}
