package com.plasstech.lang.d2.type;

public class VariableSymbol extends AbstractSymbol {
  private final SymbolStorage storage;
  private RecordSymbol recordSymbol;

  public VariableSymbol(String name, SymbolStorage storage) {
    super(name);
    this.storage = storage;
  }

  @Override
  public SymbolStorage storage() {
    return storage;
  }

  public VariableSymbol setRecordSymbol(RecordSymbol recordSymbol) {
    this.recordSymbol = recordSymbol;
    return this;
  }

  public RecordSymbol recordSymbol() {
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
}
