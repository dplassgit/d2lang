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

public class SymbolTable {
  /*
   * Only stores a single Symbol per name, so overloading (i.e., a record and proc and variable with
   * the same name) is not supported.
   */
  private final Map<String, Symbol> values = new HashMap<>();
  private final SymbolTable parent;
  private final SymbolStorage storage;

  public SymbolTable() {
    this(null, SymbolStorage.GLOBAL);
  }

  public SymbolTable(SymbolTable parent, SymbolStorage storage) {
    this.parent = parent;
    this.storage = storage;
  }

  private SymbolTable spawn() {
    return new SymbolTable(this, SymbolStorage.LOCAL);
  }

  /**
   * Looks up the symbol in this symbol table. If not found, returns VarType.UNKNOWN
   */
  public VarType lookup(String name) {
    Symbol sym = get(name);
    if (sym == null) {
      return VarType.UNKNOWN;
    }
    return sym.varType();
  }

  /**
   * Looks up the symbol in this symbol table, and if it's not found, asks its parent.
   *
   * If not found in any symbol table, returns VarType.UNKNOWN
   */
  public VarType lookupRecursive(String name) {
    Symbol sym = getRecursive(name);
    if (sym == null) {
      return VarType.UNKNOWN;
    }
    return sym.varType();
  }

  /**
   * Looks up the symbol in this symbol table. If not found, returns null;
   */
  public Symbol get(String name) {
    return values.get(name);
  }

  /**
   * Looks up the symbol in this symbol table, and if it's not found, asks its parent.
   *
   * If not found in any symbol table, returns null.
   */
  public Symbol getRecursive(String name) {
    Symbol sym = get(name);
    if (sym == null && parent != null) {
      return parent.getRecursive(name);
    }
    return sym;
  }

  /**
   * Look up the given name in this table, for a matching Symbol subclass. Example:
   *
   * <pre>
   * RecordSymbol recordSymbol = symbolTable.get(varType.name(), RecordSymbol.class, true);
   * </pre>
   *
   * @return null if no match.
   */
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

  /**
   * Look up the given name in this table, for a matching Symbol subclass. Example:
   *
   * <pre>
   * RecordSymbol recordSymbol = symbolTable.get(varType.name(), RecordSymbol.class, true);
   * </pre>
   *
   * If it's not defined in this table (or is not defined as a T) will try its parent table.
   *
   * @return null if no match.
   */
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
  public ImmutableMap<String, Symbol> variables() {
    return ImmutableMap.copyOf(
        values
            .entrySet()
            .stream()
            .filter(e -> e.getValue().isVariable())
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
  }

  /**
   * Declare a temp in this symbol table.
   */
  public VariableSymbol declareTemp(String name, VarType varType) {
    return declareVariable(name, varType, SymbolStorage.TEMP);
  }

  /**
   * Declare a param with the given index in this table.
   */
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

  /**
   * Declare an extern proc.
   */
  public ExternProcSymbol declareProc(ExternProcedureNode node) {
    Symbol sym = getRecursive(node.name());
    if (sym != null) {
      throw new TypeException(
          node.position(),
          "%s already declared as %s. Cannot be redeclared as procedure.",
          node.name(), sym.varType());
    }
    SymbolTable child = spawn();
    ExternProcSymbol procSymbol = new ExternProcSymbol(node, child);
    values.put(node.name(), procSymbol);
    return procSymbol;
  }

  /**
   * Declare a proc.
   */
  public ProcSymbol declareProc(ProcedureNode node) {
    Symbol sym = getRecursive(node.name());
    if (sym != null) {
      throw new TypeException(
          node.position(),
          "%s already declared as %s. Cannot be redeclared as PROC.",
          node.name(), sym.varType());
    }
    SymbolTable child = spawn();
    ProcSymbol procSymbol = new ProcSymbol(node, child);
    values.put(node.name(), procSymbol);
    return procSymbol;
  }

  /**
   * Creates a new block symbol for the given block node, and assign a child of this symbol table to
   * the symbol.
   */
  public BlockSymbol enterBlock(BlockNode node) {
    BlockSymbol blockSymbol = (BlockSymbol) getRecursive(node.name());
    if (blockSymbol != null) {
      return blockSymbol;
    }
    // Don't do a spawn because the block's storage must be the same as its parent's, and spawn 
    // always creates a "local" symbol table.
    SymbolTable child = new SymbolTable(this, this.storage);
    blockSymbol = new BlockSymbol(node, child);
    values.put(node.name(), blockSymbol);
    return blockSymbol;
  }

  /**
   * Declare a record in the current symbol table.
   */
  public RecordSymbol declareRecord(RecordDeclarationNode node) {
    // it was binding it with formals, e.g., Rec<T: unbound>
    // but that will not let us look it up later in a NewNode which doesn't
    // have the formals, so we store it by baseName
    String name = node.baseName();
    Symbol sym = getRecursive(name);
    if (sym != null) {
      throw new TypeException(
          node.position(),
          "'%s' already declared as %s. Cannot be redeclared as RECORD.",
          node.name(), sym.varType());
    }
    RecordSymbol recordSymbol = new RecordSymbol(node);
    values.put(name, recordSymbol);
    return recordSymbol;
  }

  /**
   * Declare a bound record in the current symbol table.
   */
  public void declareBoundRecordSymbol(RecordSymbol boundRecord) {
    // We have to use the FULL name here
    Symbol sym = getRecursive(boundRecord.name());
    if (sym != null) {
      // Already declared, which is fine!
      return;
    }
    values.put(boundRecord.name(), boundRecord);
  }

  // It's only declared.
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

  /**
   * Note that the given variable is assigned to the given type. Creates the symbol.
   */
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

  public SymbolStorage storage() {
    return storage;
  }

  public SymbolTable parent() {
    return parent;
  }

  /**
   * Find the SymbolTable in which the given symbol was created.
   */
  public SymbolTable getOwner(Symbol symbol) {
    SymbolTable source = this;
    String name = symbol.varType().name();
    while (source != null && source.get(name) == null) {
      source = source.parent();
    }
    return source;
  }
}