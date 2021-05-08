package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

public class ProcedureNode extends Node {
  private final BlockNode statements;
  private final String name;

  // TODO: capture name, parameters
  ProcedureNode(String name, BlockNode statements, Position start) {
    super(Type.PROC, start);
    this.name = name; // TODO: mangle?
    this.statements = statements;
  }

  public BlockNode statements() {
    return statements;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return String.format("ProcedureNode: %s (args TODO) returns %s:\n{%s}", name(),
            "return type (TODO)", statements);
  }
}
