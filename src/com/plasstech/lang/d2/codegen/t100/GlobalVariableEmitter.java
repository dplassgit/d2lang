package com.plasstech.lang.d2.codegen.t100;

import java.util.Map;

import com.plasstech.lang.d2.codegen.Emitter;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.DefaultNodeVisitor;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.type.BlockSymbol;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.Symbol;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.VariableSymbol;

class GlobalVariableEmitter extends DefaultNodeVisitor {
  private SymbolTable mySymbolTable;
  private Emitter emitter;

  GlobalVariableEmitter(SymbolTable mySymbolTable, Emitter emitter) {
    this.mySymbolTable = mySymbolTable;
    this.emitter = emitter;
    emitGlobals(mySymbolTable);
  }

  @Override
  public void visit(ProcedureNode node) {
    ProcSymbol procSymbol = (ProcSymbol) mySymbolTable.get(node.name());
    SymbolTable globals = procSymbol.symTab();
    emitGlobals(globals);
    VarType returnType = node.returnType();
    if (returnType != VarType.VOID) {
      // add a return slot
      String zeros = T100Locations.zeros(returnType);
      emitter.addData(
          String.format("%s: db %s", T100Locations.returnSlot(node.name()), zeros));
    }
    mySymbolTable = globals;
    // recurse
    super.visit(node);
    mySymbolTable = globals.parent();
  }

  @Override
  public void visit(BlockNode node) {
    BlockSymbol blockSymbol = mySymbolTable.enterBlock(node);
    SymbolTable globals = blockSymbol.symTab();
    emitGlobals(globals);
    mySymbolTable = globals;
    // recurse
    super.visit(node);
    mySymbolTable = globals.parent();
  }

  private void emitGlobals(SymbolTable symbolTable) {
    for (Map.Entry<String, Symbol> entry : symbolTable.variables().entrySet()) {
      VariableSymbol symbol = (VariableSymbol) entry.getValue();
      // Reserve (& clear) 1 byte for bool, 4 bytes per int, 2 bytes for string (pointer), etc.
      // The t100 assembler can only deal with db; we must repeat the zero
      // for as many bytes as we need.
      String zeros = T100Locations.zeros(symbol.varType());
      if (symbol.storage() == SymbolStorage.GLOBAL) {
        emitter.addData(String.format("_%s: db %s", entry.getKey(), zeros));
      } else if (symbol.storage() != SymbolStorage.TEMP) {
        String name = T100Locations.locationName(symbol);
        emitter.addData(String.format("%s: db %s", name, zeros));
      }
    }
  }
}
