package com.plasstech.lang.d2.parse;

import java.util.List;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.common.NodeVisitor;

/**
 * Represents a list of statements - a program, or a block inside a
 * function/method/procedure.
 */
public class StatementsNode extends Node {
  private final List<StatementNode> children;

  StatementsNode(List<StatementNode> children) {
    super(Type.STATEMENTS, children.get(0).position());
    this.children = children;
  }

  public List<StatementNode> children() {
    return children;
  }

  @Override
  public String toString() {
    return Joiner.on("\n").join(children);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    children().forEach(node -> node.accept(visitor));
  }
}
