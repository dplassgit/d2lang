package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.DefaultNodeVisitor;
import com.plasstech.lang.d2.parse.node.ProcedureNode;

/**
 * Finds all variables and parameters and if they are records, set their RecordSymbol. This is
 * needed so that the record type is set from the correct sub-symbol table, and it's more difficult
 * to do in the StaticChecker or SymbolTable or ILCodeGenerator.
 */
class RecordTypeAssigner extends DefaultNodeVisitor {
  private SymbolTable symbolTable;

  public RecordTypeAssigner(SymbolTable symbolTable) {
    this.symbolTable = symbolTable;
    maybeSetRecordSymbols();
  }

  @Override
  public void visit(ProcedureNode node) {
    ProcSymbol procSymbol = (ProcSymbol) symbolTable.get(node.name());
    symbolTable = procSymbol.symTab();
    // for each parameter, set its record symbol. 
    procSymbol.formals().stream().forEach(param -> {
      maybeSetRecordSymbol(param);
    });
    super.visit(node);
    symbolTable = symbolTable.parent();
  }

  private void maybeSetRecordSymbol(VariableSymbol variable) {
    VarType varType = variable.varType();
    if (varType.isRecord()) {
      RecordSymbol recordSymbol = (RecordSymbol) symbolTable.getRecursive(varType.name());
      if (recordSymbol != null) {
        variable.setRecordSymbol(recordSymbol);
      }
    }
  }

  @Override
  public void visit(BlockNode node) {
    BlockSymbol blockSymbol = symbolTable.enterBlock(node);
    symbolTable = blockSymbol.symTab();
    maybeSetRecordSymbols();
    super.visit(node);
    symbolTable = symbolTable.parent();
  }

  private void maybeSetRecordSymbols() {
    symbolTable.variables().forEach((name, symbol) -> {
      VariableSymbol variable = (VariableSymbol) symbol;
      maybeSetRecordSymbol(variable);
    });
  }
}
