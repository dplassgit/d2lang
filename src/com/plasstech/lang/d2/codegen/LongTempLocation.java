package com.plasstech.lang.d2.codegen;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.VariableSymbol;

public class LongTempLocation extends TempLocation {

  public static LongTempLocation create(String fullName, VarType type) {
    VariableSymbol symbol = new VariableSymbol(fullName, SymbolStorage.LONG_TEMP);
    symbol.setVarType(type);
    LongTempLocation temp = new LongTempLocation(symbol);
    return temp;
  }

  public LongTempLocation(VariableSymbol symbol) {
    super(symbol);
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.LONG_TEMP;
  }
}
