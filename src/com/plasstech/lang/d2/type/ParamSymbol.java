package com.plasstech.lang.d2.type;

public class ParamSymbol extends VariableSymbol {

  private final int index;

  // need index
  public ParamSymbol(String name, int index) {
    super(name, SymbolStorage.PARAM);
    this.index = index;
  }

  public int index() {
    return index;
  }
}
