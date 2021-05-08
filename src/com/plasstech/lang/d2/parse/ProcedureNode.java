package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

public class ProcedureNode extends Node {
  private final BlockNode statements;

  // TODO: capture parameters
  ProcedureNode(BlockNode statements, Position start) {
    super(Type.PROC, start);
    this.statements = statements;
  }

  public BlockNode statements() {
    return statements;
  }
}
