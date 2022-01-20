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
    switch(index()) {
      case 0:
      case 1:
      case 2:
      case 3:
        return SymbolStorage.REGISTER;
      default:
        return SymbolStorage.LOCAL;
    }
  }

  public int index() {
    return index;
  }
}
