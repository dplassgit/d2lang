package com.plasstech.lang.d2.type;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.parse.node.ProcedureNode;

/** Symbol Table. */
public class SymTab {

  private final Map<String, Symbol> values = new HashMap<>();
  private final SymTab parent;
  private SymbolStorage storage;

  public SymTab() {
    this.parent = null;
    this.storage = SymbolStorage.GLOBAL;
  }

  private SymTab(SymTab parent) {
    this.parent = parent;
    this.storage = SymbolStorage.LOCAL;
  }

  public SymTab spawn() {
    return new SymTab(this);
  }

  public boolean isAssigned(String name) {
    Symbol sym = values.get(name);
    return sym != null && sym.isAssigned();
  }

  public VarType lookup(String name) {
    return lookup(name, true);
  }

  public VarType lookup(String name, boolean inherit) {
    Symbol sym;
    if (inherit) {
      sym = getRecursive(name);
    } else {
      sym = get(name);
    }
    if (sym == null) {
      return VarType.UNKNOWN;
    }
    return sym.type();
  }

  public Symbol getRecursive(String name) {
    Symbol sym = values.get(name);
    if (sym == null && parent != null) {
      return parent.getRecursive(name);
    }
    return sym;
  }

  public Symbol get(String name) {
    return values.get(name);
  }

  public ImmutableMap<String, Symbol> entries() {
    return ImmutableMap.copyOf(values);
  }

  public Symbol declareTemp(String name, VarType varType) {
    return declareInternal(name, varType);
  }

  public Symbol declareParam(String name, VarType varType) {
    Symbol param = declareInternal(name, varType);
    param.setAssigned(); // parameters are always assigned, by definition.
    return param;
  }

  public ProcSymbol declareProc(ProcedureNode node) {
    Symbol sym = getRecursive(node.name());
    if (sym != null) {
      throw new TypeException(
              String.format("%s already declared as %s. Cannot be redeclared as procedure.",
                      node.name(), sym.type()),
              node.position());
    }
    ProcSymbol procSymbol = new ProcSymbol(node);
    values.put(node.name(), procSymbol);
    return procSymbol;
  }

  // It's only declared. TODO: distinguish between locals & globals
  public Symbol declare(String name, VarType varType) {
    return declareInternal(name, varType);
  }

  private Symbol declareInternal(String name, VarType varType) {
    Preconditions.checkState(!values.containsKey(name),
            "%s already declared as %s. Cannot be redeclared as %s.", name, values.get(name),
            varType);
//    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set type of %s to unknown", name);
    Symbol sym = new VariableSymbol(name).setType(varType);
    values.put(name, sym);
    return sym;
  }

  public Symbol assign(String name, VarType varType) {
    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set type of %s to unknown", name);
    Symbol sym = values.get(name);
    if (sym != null && !sym.type().isUnknown()) {
      Preconditions.checkState(sym.type() == varType,
              "Type error: %s already declared as %s. Cannot be assigned as %s.", name, sym.type(),
              varType);
    } else {
      sym = new VariableSymbol(name).setType(varType);
    }
    sym.setAssigned();
    values.put(name, sym);
    return sym;
  }

  @Override
  public String toString() {
    return values.values().toString();
  }
}
