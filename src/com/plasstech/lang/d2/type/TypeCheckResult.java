package com.plasstech.lang.d2.type;

public class TypeCheckResult {
  private final SymTab symTab;
  private final String message;

  public TypeCheckResult(SymTab symTab) {
    this.symTab = symTab;
    this.message = null;
  }

  public TypeCheckResult(String errorMessage) {
    this.symTab = null;
    this.message = errorMessage;
  }

  public SymTab symbolTable() {
    if (isError()) {
      throw new IllegalStateException("Cannot get symbol table from error result");
    }
    return symTab;
  }

  public boolean isError() {
    return message != null;
  }

  public String message() {
    return message;
  }
}
