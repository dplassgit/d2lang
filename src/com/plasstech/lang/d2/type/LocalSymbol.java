package com.plasstech.lang.d2.type;

public class LocalSymbol extends VariableSymbol {

  private int offset;

  public LocalSymbol(SymbolTable st, String name, SymbolStorage storage) {
    super(st, name, storage);
  }

  /** this is ALWAYS positive */
  public int offset() {
    return offset;
  }

  public LocalSymbol setOffset(int offset) {
    this.offset = offset;
    return this;
  }
}
