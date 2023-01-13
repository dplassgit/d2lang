package com.plasstech.lang.d2.codegen.t100;

import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.VariableSymbol;

final class T100Locations {
  static String zeros(VarType type) {
    return "0x00,".repeat(type.size() - 1) + "0x00";
  }

  static String returnSlot(String procName) {
    return "_RETURN_SLOT_OF_" + procName;
  }

  static String locationName(VariableSymbol symbol) {
    if (symbol.storage() == SymbolStorage.GLOBAL) {
      return "_" + symbol.name();
    }
    return "_" + symbol.storage().name() + "_" + symbol.getParentName() + "_" + symbol.name();
  }

  private T100Locations() {}
}
