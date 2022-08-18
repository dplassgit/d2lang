package com.plasstech.lang.d2.type;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.plasstech.lang.d2.parse.node.DefaultNodeVisitor;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode.Parameter;

/**
 * "Gathers" all procedure definitions in the (global) symbol table, so you can make
 * forward-references.
 */
class ProcGatherer extends DefaultNodeVisitor {
  private final SymTab symbolTable;

  public ProcGatherer(SymTab symbolTable) {
    this.symbolTable = symbolTable;
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

    // 2. spawn symbol table & assign to the node.
    SymTab child = symbolTable.spawn();
    procSymbol.setSymTab(child);

    // 4. add all formals to proc's symbol table
    int i = 0;
    for (Parameter formal : node.parameters()) {
      child.declareParam(formal.name(), formal.varType(), i++);
    }
  }
}
