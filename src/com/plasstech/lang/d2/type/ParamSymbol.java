package com.plasstech.lang.d2.type;

public class ParamSymbol extends VariableSymbol {

  private final int index;
  private int offset;

  public ParamSymbol(SymbolTable symtab, String name, int index) {
    super(symtab, name, SymbolStorage.PARAM);
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
