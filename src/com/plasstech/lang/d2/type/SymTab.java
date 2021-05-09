package com.plasstech.lang.d2.type;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/** Symbol Table. */
public class SymTab {

  private final Map<String, VarType> values = new HashMap<>();
  private SymTab parent;

  public SymTab() {
  }

  private SymTab(SymTab parent) {
    this.parent = parent;
  }

  public SymTab spawn() {
    return new SymTab(this);
  }

  public VarType lookup(String name) {
    VarType varType = values.getOrDefault(name, VarType.UNKNOWN);
    if (!varType.isUnknown()) {
      return varType;
    }
    if (parent != null) {
      return parent.lookup(name);
    } else {
      return VarType.UNKNOWN;
    }
  }

  public ImmutableMap<String, VarType> entries() {
    return ImmutableMap.copyOf(values);
  }

  public void add(String name, VarType varType) {
    Preconditions.checkState(!values.containsKey(name), "Already recorded type for %s", name);
    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set type of %s to unknown", name);
    values.put(name, varType);
  }

}
