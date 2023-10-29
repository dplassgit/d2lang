package com.plasstech.lang.d2.type;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.parse.node.ProcedureNode;

public class ProcSymbol extends AbstractSymbol {

  private final ProcedureNode node;
  private final List<ParamSymbol> formals = new ArrayList<>();
  private final SymbolTable symtab;

  public ProcSymbol(ProcedureNode node, SymbolTable symTab) {
    super(node.name());
    this.node = node;
    symtab = symTab;
    this.setVarType(VarType.PROC);
  }

  public String mungedName() {
    return "_" + super.name();
  }

  @Override
  public String toString() {
    return String.format(
        "%s: proc(%s): %s", name(), formals.toString(), node.returnType().toString());
  }

  @Override
  public SymbolStorage storage() {
    // TODO: this might be a local if it's nested
    return SymbolStorage.GLOBAL;
  }

  public SymbolTable symTab() {
    return symtab;
  }

  // Methods that hide the fact that we have a node backing
  public VarType returnType() {
    return node.returnType();
  }

  public ImmutableList<ParamSymbol> formals() {
    return ImmutableList.copyOf(formals);
  }

  public void declareParam(String name, VarType varType, int index) {
    ParamSymbol param = symtab.declareParam(name, varType, index);
    param.setParentName(name());
    // keep a copy!
    formals.add(param);
  }

  public ParamSymbol formal(int i) {
    return formals.get(i);
  }

  public Position position() {
    return node.position();
  }

  public boolean isExtern() {
    return false;
  }
}
