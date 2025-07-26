package com.plasstech.lang.d2.type;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.ExternProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;

/** Symbol Table. */
public class SymTab implements SymbolTable {

  /*
   * Only stores a single Symbol per name, so overloading (i.e., a record and proc and variable with
   * the same name) is not supported.
   */
  private final Map<String, Symbol> values = new HashMap<>();
  private final SymbolTable parent;
  private final SymbolStorage storage;

  public SymTab() {
    this(null, SymbolStorage.GLOBAL);
  }

  public SymTab(SymbolTable parent, SymbolStorage storage) {
    this.parent = parent;
    this.storage = storage;
  }

  private SymTab spawn() {
    return new SymTab(this, SymbolStorage.LOCAL);
  }

  @Override
  public VarType lookup(String name) {
    Symbol sym = get(name);
    if (sym == null) {
      return VarType.UNKNOWN;
    }
    return sym.varType();
  }

  @Override
  public VarType lookupRecursive(String name) {
    Symbol sym = getRecursive(name);
    if (sym == null) {
      return VarType.UNKNOWN;
    }
    return sym.varType();
  }

  @Override
  public Symbol get(String name) {
    return values.get(name);
  }

  @Override
  public Symbol getRecursive(String name) {
    Symbol sym = get(name);
    if (sym == null && parent != null) {
      return parent.getRecursive(name);
    }
    return sym;
  }

  @Override
  public <T extends Symbol> T get(String name, Class<T> clazz) {
    Symbol sym = values.get(name);
    // possibilities are:
    // 1. wrong type or null
    // 2. good!
    if (!isInstance(sym, clazz)) {
      // wrong type, or null
      return null;
    }
    return (T) sym;
  }

  @Override
  public <T extends Symbol> T getRecursive(String name, Class<T> clazz) {
    Symbol sym = get(name, clazz);
    // possibilities are:
    // 1. wrong type or null: try parent
    // 2. good!
    if (sym == null && parent != null) {
      return parent.getRecursive(name, clazz);
    }
    return (T) sym;
  }

  private static <T extends Symbol> boolean isInstance(Object thing, Class<T> clazz) {
    if (thing == null) {
      return false;
    }
    // might handle subclasses (vs. equals)
    return thing.getClass().isAssignableFrom(clazz);
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
    ParamSymbol param = new ParamSymbol(this, name, index);
    param.setVarType(varType).setAssigned();
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
    // it was binding it with formals, e.g., Rec<T: unbound>
    // but that will not let us look it up later in a NewNode which doesn't
    // have the formals, so we store it by baseName
    String name = node.baseName();
    Symbol sym = getRecursive(name);
    if (sym != null) {
      throw new TypeException(
          String.format(
              "'%s' already declared as %s. Cannot be redeclared as RECORD.",
              node.name(), sym.varType()),
          node.position());
    }
    RecordSymbol recordSymbol = new RecordSymbol(node);
    values.put(name, recordSymbol);
    return recordSymbol;
  }

  @Override
  public void declareBoundRecordSymbol(RecordSymbol boundRecord, Position pos) {
    // We have to use the FULL name here
    Symbol sym = getRecursive(boundRecord.name());
    if (sym != null) {
      // Already declared, which is fine!
      return;
    }
    values.put(boundRecord.name(), boundRecord);
  }

  // It's only declared.
  @Override
  public VariableSymbol declare(String name, VarType varType) {
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
    sym.setVarType(varType);
    values.put(name, sym);
    return sym;
  }

  private VariableSymbol createVariable(String name, SymbolStorage storage) {
    if (storage == SymbolStorage.LOCAL) {
      return new LocalSymbol(this, name, storage);
    } else {
      return new VariableSymbol(this, name, storage);
    }
  }

  @Override
  public VariableSymbol assign(String name, VarType varType) {
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
    VariableSymbol variableSym = (VariableSymbol) sym;
    sym.setAssigned();
    values.put(name, sym);
    return variableSym;
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
