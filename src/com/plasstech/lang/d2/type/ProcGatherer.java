package com.plasstech.lang.d2.type;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.DefaultNodeVisitor;
import com.plasstech.lang.d2.parse.node.ExternProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;

/**
 * "Gathers" all procedure definitions in the (global) symbol table, so you can make
 * forward-references. Checks parameters too (otherwise we wouldn't be able to add to the symbol
 * table.)
 */
class ProcGatherer extends DefaultNodeVisitor {
  private SymbolTable symbolTable;

  public ProcGatherer(SymbolTable symbolTable) {
    this.symbolTable = symbolTable;
  }

  @Override
  public void visit(BlockNode node) {
    BlockSymbol blockSymbol = symbolTable.enterBlock(node);
    symbolTable = blockSymbol.symTab();
    super.visit(node);
    symbolTable = symbolTable.parent();
  }

  @Override
  public void visit(ExternProcedureNode node) {
    // 1. make sure no duplicate arg names
    List<String> paramNames =
        node.parameters().stream().map(Parameter::name).collect(toImmutableList());
    Set<String> duplicates = new HashSet<>();
    Set<String> uniques = new HashSet<>();
    for (String param : paramNames) {
      if (uniques.contains(param)) {
        uniques.remove(param);
        duplicates.add(param);
      } else {
        uniques.add(param);
      }
    }
    if (!duplicates.isEmpty()) {
      throw new TypeException(
          String.format(
              "Duplicate parameter names: %s in procedure %s", duplicates.toString(), node.name()),
          node.position());
    }

    // Add this procedure to the symbol table
    ExternProcSymbol procSymbol = symbolTable.declareProc(node);

    // 4. add all formals to proc's symbol table
    int i = 0;
    for (Parameter formal : node.parameters()) {
      procSymbol.declareParam(formal.name(), formal.varType(), i++);
    }
  }

  @Override
  public void visit(ProcedureNode node) {
    // 1. make sure no duplicate arg names
    List<String> paramNames =
        node.parameters().stream().map(Parameter::name).collect(toImmutableList());
    Set<String> duplicates = new HashSet<>();
    Set<String> uniques = new HashSet<>();
    for (String param : paramNames) {
      if (uniques.contains(param)) {
        uniques.remove(param);
        duplicates.add(param);
      } else {
        uniques.add(param);
      }
    }
    if (!duplicates.isEmpty()) {
      throw new TypeException(
          String.format(
              "Duplicate parameter names: %s in procedure %s", duplicates.toString(), node.name()),
          node.position());
    }

    // Add this procedure to the symbol table
    ProcSymbol procSymbol = symbolTable.declareProc(node);
    symbolTable = procSymbol.symTab();
    // Recurse, so the block's symbol table has this one as a parent.
    super.visit(node);
    symbolTable = symbolTable.parent();

    // add all formals to proc's symbol table
    int i = 0;
    for (Parameter formal : node.parameters()) {
      procSymbol.declareParam(formal.name(), formal.varType(), i++);
    }
  }
}
