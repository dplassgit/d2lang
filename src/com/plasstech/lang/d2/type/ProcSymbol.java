package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.parse.node.ProcedureNode;

public class ProcSymbol extends AbstractSymbol {

  private final ProcedureNode node;
  private SymTab symtab;

  public ProcSymbol(ProcedureNode node) {
    super(node.name());
    this.node = node;
    this.setType(VarType.PROC); // TODO: make this into its complex type!!!
  }

  // Maybe think about this?
  public ProcedureNode node() {
    return node;
  }

  @Override
  public String toString() {
    return String.format("%s: proc(%s): %s", name(), node.parameters().toString(),
            node.returnType().toString());
  }

  public void setSymTab(SymTab symtab) {
    this.symtab = symtab;
  }

  public SymTab symTab() {
    return symtab;
  }

  public VarType returnType() {
    return node().returnType();
  }
}
