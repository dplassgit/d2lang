package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.parse.ProcedureNode;

public class ProcSymbol extends AbstractSymbol {

  private final ProcedureNode node;

  public ProcSymbol(ProcedureNode node) {
    super(node.name());
    this.node = node;
  }

  // Maybe think about this?
  public ProcedureNode procedureNode() {
    return node;
  }
}
