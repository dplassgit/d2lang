package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.parse.node.ProcedureNode;

public class ExternProcSymbol extends ProcSymbol {

  public ExternProcSymbol(ProcedureNode node, SymbolTable symTab) {
    super(node, symTab);
  }

  @Override
  public String mungedName() {
    return name();
  }

  @Override
  public String toString() {
    return String.format("%s: extern proc(%s): %s", name(), formals(), returnType());
  }

  @Override
  public boolean isExtern() {
    return true;
  }
}
