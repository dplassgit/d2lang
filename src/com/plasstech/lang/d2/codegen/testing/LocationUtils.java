package com.plasstech.lang.d2.codegen.testing;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.LongTempLocation;
import com.plasstech.lang.d2.codegen.MemoryAddress;
import com.plasstech.lang.d2.codegen.ParamLocation;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.type.LocalSymbol;
import com.plasstech.lang.d2.type.ParamSymbol;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.VariableSymbol;

public class LocationUtils {
  public static ParamLocation newParamLocation(String name, VarType type, int index, int offset) {
    ParamSymbol symbol = new ParamSymbol(name, index).setOffset(offset);
    symbol.setVarType(type);
    return new ParamLocation(symbol);
  }

  public static StackLocation newStackLocation(String name, VarType type, int offset) {
    LocalSymbol symbol = new LocalSymbol(name, SymbolStorage.LOCAL).setOffset(offset);
    symbol.setVarType(type);
    return new StackLocation(symbol);
  }

  public static MemoryAddress newMemoryAddress(String name, VarType varType) {
    VariableSymbol symbol = new VariableSymbol(name, SymbolStorage.GLOBAL);
    symbol.setVarType(varType);
    return new MemoryAddress(symbol);

  }

  public static TempLocation newTempLocation(String name, VarType varType) {
    VariableSymbol symbol = new VariableSymbol(name, SymbolStorage.TEMP);
    symbol.setVarType(varType);
    return new TempLocation(symbol);
  }

  public static Location newLongTempLocation(String name, VarType varType) {
    VariableSymbol symbol = new VariableSymbol(name, SymbolStorage.LONG_TEMP);
    symbol.setVarType(varType);
    return new LongTempLocation(symbol);
  }
}
