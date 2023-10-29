package com.plasstech.lang.d2.type;

public class VariableSymbol extends AbstractSymbol {
  private final SymbolStorage storage;
  private RecordSymbol recordSymbol;
  private String parentName;

  // TODO: give the symbol its enclosing SymbolTable
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

  public void setParentName(String name) {
    this.parentName = name;
  }

  public String getParentName() {
    return parentName;
  }
}
