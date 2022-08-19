package com.plasstech.lang.d2.type;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;

public class ProcSymbol extends AbstractSymbol {

  private final ProcedureNode node;
  private SymTab symtab;

  public ProcSymbol(ProcedureNode node) {
    super(node.name());
    this.node = node;
    this.setVarType(VarType.PROC);
  }

  public String mungedName() {
    return "_" + super.name();
  }

  @Override
  public String toString() {
    return String.format(
        "%s: proc(%s): %s", name(), node.parameters().toString(), node.returnType().toString());
  }

  @Override
  public SymbolStorage storage() {
    // TODO: this might be a local if it's nested
    return SymbolStorage.GLOBAL;
  }

  public void setSymTab(SymTab symtab) {
    this.symtab = symtab;
  }

  public SymTab symTab() {
    return symtab;
  }

  // Methods that hide the fact that we have a node backing
  public VarType returnType() {
    return node.returnType();
  }

  public ImmutableList<Parameter> parameters() {
    return node.parameters();
  }

  public Position position() {
    return node.position();
  }
}
