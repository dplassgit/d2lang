package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;

/**
 * Breaks out of a while loop
 */
public class BreakNode extends AbstractNode implements StatementNode {

  public BreakNode(Position position) {
    super(position);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "BreakNode";
  }
}
