package com.plasstech.lang.d2.parse;

import java.util.List;

import com.google.common.base.Joiner;

public class StatementsNode extends Node {

  private final List<StatementNode> children;

  StatementsNode(List<StatementNode> children) {
    super(Type.STATEMENTS);
    this.children = children;
  }

  public List<StatementNode> children() {
    return children;
  }

  @Override
  public String toString() {
    return Joiner.on("\n").join(children);
  }
}
