package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

/** "Continue" in a while loop */
public class ContinueNode extends AbstractNode implements StatementNode {

  ContinueNode(Position position) {
    super(position);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "ContinueNode";
  }
}
