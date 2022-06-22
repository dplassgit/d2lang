package com.plasstech.lang.d2.type;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.ParamLocation;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;

public class ProcSymbol extends AbstractSymbol {

  private final ProcedureNode node;
  private SymTab symtab;

  public ProcSymbol(ProcedureNode node) {
    super(node.name());
    this.node = node;
    this.setVarType(VarType.PROC); // TODO: make this into its complex type?
  }

  // TODO: Think about this? Is it exposing too much?
  public ProcedureNode node() {
    return node;
  }

  @Override
  public String toString() {
    return String.format(
        "%s: proc(%s): %s", name(), node.parameters().toString(), node.returnType().toString());
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

  @Override
  public SymbolStorage storage() {
    // TODO: this might be a local if it's nested
    return SymbolStorage.GLOBAL;
  }

  public ImmutableList<Location> formals() {
    ImmutableList<Parameter> formalParams = node().parameters();

    ImmutableList.Builder<Location> formals = ImmutableList.builder();
    int i = 0;
    for (Parameter formal : formalParams) {
      formals.add(new ParamLocation(formal.name(), formal.varType(), i++));
    }
    return formals.build();
  }
}
