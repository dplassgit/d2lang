package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;

/** Provides all of standard.in as the result. */
public class InputNode extends AbstractNode implements ExprNode {

  public InputNode(Position position) {
    super(position);
  }

  //@Override
  //public void accept(NodeVisitor visitor) {
    //visitor.visit(this);
  //}

  @Override
  public String toString() {
    return "InputNode";
  }
}
