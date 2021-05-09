package com.plasstech.lang.d2.parse;

import java.util.List;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

/**
 * Represents a list of statements - a block inside a function/method/procedure.
 */
public class BlockNode extends Node {
  private final List<StatementNode> statements;

  BlockNode(List<StatementNode> statements, Position position) {
    super(Type.BLOCK, position);
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
