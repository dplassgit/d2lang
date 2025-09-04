package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Position;

/** Represents a list of statements - a block inside a function/method/procedure. */
public class BlockNode extends AbstractNode {
  public static final BlockNode EMPTY = new BlockNode(new Position(0, 0));
  // attempting to make a unique hashcode
  private static int id = 0;

  private final List<StatementNode> statements;
  private final String name;

  BlockNode(Position position) {
    this(ImmutableList.of(), position);
  }

  public BlockNode(List<StatementNode> statements, Position position) {
    super(position);
    this.name = String.format("block @ %d, %d (%d)", position.line(), position.column(),
        id++);
    this.statements = statements;
  }

  public List<StatementNode> statements() {
    return statements;
  }

  @Override
  public String toString() {
    return "\n" + Joiner.on('\n').join(statements);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  public String name() {
    return name;
  }
}
