package com.plasstech.lang.d2.parse;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

/**
 * Represents a list of statements - a block inside a function/method/procedure.
 */
public class BlockNode extends AbstractNode {
  private final List<StatementNode> statements;

  BlockNode(Position position) {
    super(position);
    this.statements = ImmutableList.of();
  }

  BlockNode(List<StatementNode> statements, Position position) {
    super(position);
    this.statements = statements;
  }

  public List<StatementNode> statements() {
    return statements;
  }

  @Override
  public String toString() {
    return Joiner.on("\n// ").join(statements);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    statements().forEach(node -> node.accept(visitor));
  }
}
