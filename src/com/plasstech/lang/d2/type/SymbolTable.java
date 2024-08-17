package com.plasstech.lang.d2.type;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.ExternProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;

public interface SymbolTable {

  /**
   * Looks up the symbol in this symbol table, and if it's not found, asks its parent.
   *
   * If not found in any symbol table, returns VarType.UNKNOWN
   */
  VarType lookupRecursive(String name);

  /**
   * Looks up the symbol in this symbol table. If not found, returns VarType.UNKNOWN
   */
  VarType lookup(String name);

  /**
   * Looks up the symbol in this symbol table, and if it's not found, asks its parent.
   *
   * If not found in any symbol table, returns null.
   */
  Symbol getRecursive(String name);

  /**
   * Looks up the symbol in this symbol table. If not found, returns null;
   */
  Symbol get(String name);

  /**
   * Look up the given name in this table, for a matching Symbol type. Example:
   *
   * <pre>
   * RecordSymbol recordSymbol = symbolTable.get(varType.name(), RecordSymbol.class, true);
   * </pre>
   *
   * If it's not defined in this table (or is not defined as a T) will try its parent table.
   *
   * @return null if no match.
   */
  <T extends Symbol> T get(String name, Class<T> clazz);

  <T extends Symbol> T getRecursive(String name, Class<T> clazz);

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

  // It's assigned with the given type.
  VariableSymbol assign(String name, VarType varType);

  SymbolStorage storage();

  SymbolTable parent();
}