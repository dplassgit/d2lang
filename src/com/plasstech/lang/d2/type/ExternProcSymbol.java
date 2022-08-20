package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.parse.node.ProcedureNode;

public class ExternProcSymbol extends ProcSymbol {

  public ExternProcSymbol(ProcedureNode node) {
    super(node);
  }

  @Override
  public String mungedName() {
    return name();
  }

  @Override
  public String toString() {
    return String.format("%s: extern proc(%s): %s", name(), parameters(), returnType());
  }

  @Override
  public void setSymTab(SymTab symtab) {
    // yeah I know this is a violation of LSP
    throw new IllegalStateException("Should not try to set symbol table of EXTERN PROC");
  }

  @Override
  public SymTab symTab() {
    // yeah I know this is a violation of LSP
    throw new IllegalStateException("Should not try to get symbol table of EXTERN PROC");
  }

  @Override
  public boolean isExtern() {
    return true;
  }
}
