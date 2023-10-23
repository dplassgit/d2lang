package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.phase.Errors;

public class TypeCheckResult {
  private final SymbolTable symTab;
  private Errors errors;

  public TypeCheckResult(SymbolTable symTab) {
    this.symTab = symTab;
  }

  public TypeCheckResult(D2RuntimeException exception) {
    this.symTab = null;
    this.errors = new Errors();
    errors.add(exception);
  }

  public TypeCheckResult(Errors errors) {
    this.symTab = null;
    this.errors = errors;
  }

  public SymbolTable symbolTable() {
    if (isError()) {
      throw new IllegalStateException("Cannot get symbol table from error result");
    }
    return symTab;
  }

  public boolean isError() {
    return errors != null && errors.hasErrors();
  }

  public Errors errors() {
    return errors;
  }
}
