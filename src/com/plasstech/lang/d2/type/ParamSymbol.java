package com.plasstech.lang.d2.type;

public class ParamSymbol extends VariableSymbol {

  private final int index;
  private int offset;

  public ParamSymbol(String name, int index) {
    super(name, SymbolStorage.PARAM);
    this.index = index;
  }

  public int index() {
    return index;
  }

  public ParamSymbol setOffset(int offset) {
    this.offset = offset;
    return this;
  }

  public int offset() {
    return offset;
  }
}
