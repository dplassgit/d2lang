package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.common.D2RuntimeException;

public class TypeCheckResult {
  private final SymTab symTab;
  private final D2RuntimeException exception;

  public TypeCheckResult(SymTab symTab) {
    this.symTab = symTab;
    this.exception = null;
  }

  public TypeCheckResult(D2RuntimeException exception) {
    this.symTab = null;
    this.exception = exception;
  }

  public SymTab symbolTable() {
    if (isError()) {
      throw new IllegalStateException("Cannot get symbol table from error result");
    }
    return symTab;
  }

  public boolean isError() {
    return exception != null;
  }

  public D2RuntimeException exception() {
    return exception;
  }
}
