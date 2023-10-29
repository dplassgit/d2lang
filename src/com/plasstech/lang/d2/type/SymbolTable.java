package com.plasstech.lang.d2.type;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.ExternProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;

public interface SymbolTable {

  boolean isAssigned(String name);

  VarType lookup(String name);

  VarType lookup(String name, boolean recurse);

  Symbol getRecursive(String name);

  Symbol get(String name);

  /** Returns all symbols in this level of the table. */
  ImmutableMap<String, Symbol> entries();

  /** Returns all the variables in this level of the table. */
  ImmutableMap<String, Symbol> variables();

  VariableSymbol declareTemp(String name, VarType varType);

  ParamSymbol declareParam(String name, VarType varType, int index);

  ExternProcSymbol declareProc(ExternProcedureNode node);

  ProcSymbol declareProc(ProcedureNode node);

  BlockSymbol enterBlock(BlockNode node);

  RecordSymbol declareRecord(RecordDeclarationNode node);

  // It's only declared.
  VariableSymbol declare(String name, VarType varType);

  VariableSymbol assign(String name, VarType varType);

  SymbolStorage storage();

  SymbolTable parent();

}