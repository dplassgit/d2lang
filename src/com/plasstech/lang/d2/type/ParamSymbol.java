package com.plasstech.lang.d2.type;

public class ParamSymbol extends AbstractSymbol {

  private final int index;

  // need index
  public ParamSymbol(String name, int index) {
    super(name);
    this.index = index;
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.PARAM;
  }

  public int index() {
    return index;
  }
}
