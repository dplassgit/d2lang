package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

public class ProgramNode extends Node {

  private final BlockNode statements;
  private final ProcedureNode main;

  ProgramNode(BlockNode statements, ProcedureNode main) {
    super(Type.MAIN, new Position(0, 0));
    this.statements = statements;
    this.main = main;
  }

  public BlockNode statements() {
    return statements;
  }

  public ProcedureNode main() {
    return main;
  }
}
