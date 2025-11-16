package com.plasstech.lang.d2.type;

import com.google.common.base.Preconditions;

public class VariableSymbol extends AbstractSymbol {
  private final SymbolStorage storage;
  private final SymbolTable symbolTable;
  private RecordSymbol recordSymbol;

  // TODO: maybe get rid of SymbolStorage, because it should be in SymbolTable?
  public VariableSymbol(SymbolTable symbolTable, String name, SymbolStorage storage) {
    super(name);
    this.symbolTable = symbolTable;
    this.storage = storage;
  }

  @Override
  public SymbolStorage storage() {
    return storage;
  }

  public RecordSymbol recordSymbol() {
    Preconditions.checkState(
        varType().isRecord(),
        String.format(
            "Cannot call VariableSymbol.recordSymbol on %s; not a record type", this.name()));

    if (recordSymbol == null) {
      RecordReferenceType rrt = (RecordReferenceType) varType();
      String recordName = rrt.fqName();
      recordSymbol = symbolTable().getRecursive(recordName, RecordSymbol.class);
      if (recordSymbol == null) {
        throw new IllegalStateException("Record " + recordName + " not found in symtab");
      }
    }
    return recordSymbol;
  }

  @Override
  public String toString() {
    return String.format("%s: %s", name(), varType().toString());
  }

  @Override
  public boolean isVariable() {
    return true;
  }

  // TODO: Maybe get rid of this and replace with some kind of "clone" method?
  // clone(String newName)?
  public SymbolTable symbolTable() {
    return symbolTable;
  }
}
