package com.plasstech.lang.d2.type;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/** Symbol Table. */
public class SymTab {

  private final Map<String, Symbol> values = new HashMap<>();
  private final SymTab parent;

  public SymTab() {
    this.parent = null;
  }

  private SymTab(SymTab parent) {
    this.parent = parent;
  }

  public SymTab spawn() {
    return new SymTab(this);
  }

  public boolean isAssigned(String name) {
    Symbol sym = values.get(name);
    return sym != null && sym.isAssigned();
  }

  public VarType lookup(String name) {
    Symbol sym = values.get(name);
    if (sym == null) {
      if (parent != null) {
        return parent.lookup(name);
      } else {
        return VarType.UNKNOWN;
      }
    }
    return sym.type();
  }

  public Symbol get(String name) {
    return values.get(name);
  }

  public ImmutableMap<String, Symbol> entries() {
    return ImmutableMap.copyOf(values);
  }

  // It's only declared.
  public void declare(String name, VarType varType) {
    Preconditions.checkState(!values.containsKey(name),
            "Type error: %s already declared as %s. Cannot be redeclared as %s.", name,
            values.get(name), varType);
    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set type of %s to unknown", name);
    Symbol sym = new Symbol(name).setType(varType);
    values.put(name, sym);
  }

  public void assign(String name, VarType varType) {
    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set type of %s to unknown", name);
    Symbol sym = values.get(name);
    if (sym != null) {
      Preconditions.checkState(sym.type() == varType,
              "Type error: %s already declared as %s. Cannot be assigned as %s.", name,
              sym.type(), varType);
    } else {
      sym = new Symbol(name).setType(varType);
    }
    sym.setAssigned();
    values.put(name, sym);
  }
}
