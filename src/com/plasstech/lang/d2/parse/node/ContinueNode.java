package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;

/** "Continue" in a while loop */
public class ContinueNode extends AbstractNode implements StatementNode {

  public ContinueNode(Position position) {
    super(position);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "CONTINUE";
  }
}
