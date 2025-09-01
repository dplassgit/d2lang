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
  private final boolean isGeneric;
  private final ImmutableList<String> formalTypeVariables;

  public ProcSymbol(ProcedureNode node, SymbolTable symTab) {
    super(node.name());
    this.node = node;
    symtab = symTab;
    this.setVarType(VarType.PROC);
    this.formalTypeVariables = ImmutableList.copyOf(node.formalTypeVariables());
    this.isGeneric = node.formalTypeVariables().size() > 0;
  }

  public String mungedName() {
    return "U_" + super.name(); // U for user
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

  public boolean isGeneric() {
    return isGeneric;
  }

  public ImmutableList<String> getFormalTypeVariables() {
    return formalTypeVariables;
  }
}
