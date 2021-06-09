package com.plasstech.lang.d2.parse;

import java.util.Optional;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

public class ProgramNode extends AbstractNode {

  private final BlockNode statements;
  private final Optional<MainNode> main;

  ProgramNode(BlockNode statements) {
    this(statements, MainNode.EMPTY);
  }

  ProgramNode(BlockNode statements, MainNode main) {
    this(statements, Optional.of(main));
  }

  private ProgramNode(BlockNode statements, Optional<MainNode> maybeMain) {
    super(new Position(0, 0));
    this.statements = statements;
    this.main = maybeMain;
  }

  public BlockNode statements() {
    return statements;
  }

  public Optional<MainNode> main() {
    return main;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    statements.accept(visitor);
    if (main.isPresent()) {
      main.get().accept(visitor);
    }
  }

  @Override
  public String toString() {
    if (main.isPresent()) {
      return String.format("ProgramNode: %s\n%s", statements(), main.get());
    } else {
      return String.format("ProgramNode: %s", statements());
    }
  }
}
