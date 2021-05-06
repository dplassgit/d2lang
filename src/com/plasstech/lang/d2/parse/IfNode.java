package com.plasstech.lang.d2.parse;

import java.util.List;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

public class IfNode extends StatementNode {

  private final Node condition;
  private final List<Node> statements;

  IfNode(Node condition, List<Node> statements, Position position) {
    super(Type.IF, position);
    this.condition = condition;
    this.statements = statements;
  }

  public Node condition() {
    return condition;
  }

  public List<Node> statements() {
    return statements;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("IfNode: if (%s) { %s}", condition, statements, null);
  }
}
