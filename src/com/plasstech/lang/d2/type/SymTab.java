package com.plasstech.lang.d2.type;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.ExternProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;

/** Symbol Table. */
public class SymTab implements SymbolTable {

  private final Map<String, Symbol> values = new HashMap<>();
  private final SymbolTable parent;
  private final SymbolStorage storage;

  public SymTab() {
    this.parent = null;
    this.storage = SymbolStorage.GLOBAL;
  }

  private SymTab(SymbolTable parent, SymbolStorage storage) {
    this.parent = parent;
    this.storage = storage;
  }

  private SymTab spawn() {
    return new SymTab(this, SymbolStorage.LOCAL);
  }

  @Override
  public boolean isAssigned(String name) {
    Symbol sym = getRecursive(name);
    return sym != null && sym.isAssigned();
  }

  @Override
  public VarType lookup(String name) {
    return lookup(name, true);
  }

  @Override
  public VarType lookup(String name, boolean recurse) {
    Symbol sym;
    if (recurse) {
      sym = getRecursive(name);
    } else {
      sym = get(name);
    }
    if (sym == null) {
      return VarType.UNKNOWN;
    }
    return sym.varType();
  }

  @Override
  public Symbol getRecursive(String name) {
    Symbol sym = values.get(name);
    if (sym == null && parent != null) {
      return parent.getRecursive(name);
    }
    return sym;
  }

  @Override
  public Symbol get(String name) {
    return values.get(name);
  }

  /** Returns all symbols in this level of the table. */
  @Override
  public ImmutableMap<String, Symbol> entries() {
    return ImmutableMap.copyOf(values);
  }

  /** Returns all the variables in this level of the table. */
  @Override
  public ImmutableMap<String, Symbol> variables() {
    return ImmutableMap.copyOf(
        values
            .entrySet()
            .stream()
            .filter(e -> e.getValue().isVariable())
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
  }

  @Override
  public VariableSymbol declareTemp(String name, VarType varType) {
    return declareVariable(name, varType, SymbolStorage.TEMP);
  }

  @Override
  public ParamSymbol declareParam(String name, VarType varType, int index) {
    Preconditions.checkState(
        !values.containsKey(name),
        "%s already declared as %s. Cannot be redeclared as %s.",
        name,
        values.get(name),
        varType);
    // parameters are declared before they have a type so we can't verify that it's not unknown yet.
    //    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set type of %s to unknown",
    // name);
    // parameters are always assigned, by definition.
    ParamSymbol param = new ParamSymbol(name, index);
    param.setVarType(varType).setAssigned();
    maybeSetRecordSymbol(varType, param);
    values.put(name, param);
    return param;
  }

  @Override
  public ExternProcSymbol declareProc(ExternProcedureNode node) {
    Symbol sym = getRecursive(node.name());
    if (sym != null) {
      throw new TypeException(
          String.format(
              "%s already declared as %s. Cannot be redeclared as procedure.",
              node.name(), sym.varType()),
          node.position());
    }
    SymTab child = spawn();
    ExternProcSymbol procSymbol = new ExternProcSymbol(node, child);
    values.put(node.name(), procSymbol);
    return procSymbol;
  }

  @Override
  public ProcSymbol declareProc(ProcedureNode node) {
    Symbol sym = getRecursive(node.name());
    if (sym != null) {
      throw new TypeException(
          String.format(
              "%s already declared as %s. Cannot be redeclared as PROC.",
              node.name(), sym.varType()),
          node.position());
    }
    SymTab child = spawn();
    ProcSymbol procSymbol = new ProcSymbol(node, child);
    values.put(node.name(), procSymbol);
    return procSymbol;
  }

  @Override
  public BlockSymbol enterBlock(BlockNode node) {
    BlockSymbol blockSymbol = (BlockSymbol) getRecursive(node.name());
    if (blockSymbol != null) {
      return blockSymbol;
    }
    // Don't do a spawn because the block's storage must be the same as its parent's, and spawn 
    // always creates a "local" symbol table.
    SymbolTable child = new SymTab(this, this.storage);
    blockSymbol = new BlockSymbol(node, child);
    values.put(node.name(), blockSymbol);
    return blockSymbol;
  }

  @Override
  public RecordSymbol declareRecord(RecordDeclarationNode node) {
    Symbol sym = getRecursive(node.name());
    if (sym != null) {
      throw new TypeException(
          String.format(
              "'%s' already declared as %s. Cannot be redeclared as RECORD.",
              node.name(), sym.varType()),
          node.position());
    }
    RecordSymbol recordSymbol = new RecordSymbol(node);
    values.put(node.name(), recordSymbol);
    return recordSymbol;
  }

  // It's only declared.
  @Override
  public Symbol declare(String name, VarType varType) {
    return declareVariable(name, varType, this.storage);
  }

  private VariableSymbol declareVariable(String name, VarType varType, SymbolStorage storage) {
    Preconditions.checkState(
        !values.containsKey(name),
        "%s already declared as %s. Cannot be redeclared as %s.",
        name,
        values.get(name),
        varType);
    //    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set type of %s to unknown",
    // name);
    VariableSymbol sym = createVariable(name, storage);
    maybeSetRecordSymbol(varType, sym);
    sym.setVarType(varType);
    values.put(name, sym);
    return sym;
  }

  private static VariableSymbol createVariable(String name, SymbolStorage storage) {
    if (storage == SymbolStorage.LOCAL) {
      return new LocalSymbol(name, storage);
    } else {
      return new VariableSymbol(name, storage);
    }
  }

  @Override
  public Symbol assign(String name, VarType varType) {
    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set type of %s to unknown", name);
    Symbol sym = values.get(name);
    if (sym != null && !sym.varType().isUnknown()) {
      Preconditions.checkState(
          sym.varType() == varType,
          "Type error: %s already declared as %s. Cannot be assigned as %s.",
          name,
          sym.varType(),
          varType);
    } else {
      sym = createVariable(name, this.storage).setVarType(varType);
    }
    maybeSetRecordSymbol(varType, (VariableSymbol) sym);
    sym.setAssigned();
    values.put(name, sym);
    return sym;
  }

  private void maybeSetRecordSymbol(VarType varType, VariableSymbol sym) {
    if (varType.isRecord()) {
      RecordSymbol recordSymbol = (RecordSymbol) getRecursive(varType.name());
      if (recordSymbol != null) {
        sym.setRecordSymbol(recordSymbol);
      }
    }
  }

  @Override
  public String toString() {
    return values.toString();
  }

  @Override
  public SymbolStorage storage() {
    return storage;
  }

  @Override
  public SymbolTable parent() {
    return parent;
  }
}
