package com.plasstech.lang.d2.parse;

import java.util.Optional;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

public class ProgramNode extends Node {

  private final BlockNode statements;
  private final Optional<ProcedureNode> main;

  ProgramNode(BlockNode statements) {
    this(statements, Optional.empty());
  }

  ProgramNode(BlockNode statements, ProcedureNode main) {
    this(statements, Optional.of(main));
  }

  public ProgramNode(BlockNode statements, Optional<ProcedureNode> maybeMain) {
    super(Type.PROGRAM, new Position(0, 0));
    this.statements = statements;
    this.main = maybeMain;
  }

  public BlockNode statements() {
    return statements;
  }

  public Optional<ProcedureNode> main() {
    return main;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    statements.accept(visitor);
    if (main.isPresent()) {
      main.get().accept(visitor);
    }
  }
}
