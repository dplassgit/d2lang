package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

public class BreakNode extends AbstractNode implements StatementNode {

  BreakNode(Position position) {
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
