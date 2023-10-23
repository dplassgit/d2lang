package com.plasstech.lang.d2.type;

public class LocalSymbol extends VariableSymbol {

  private int offset;

  public LocalSymbol(String name, SymbolStorage storage) {
    super(name, storage);
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
