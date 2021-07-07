package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;

public class VariableSetNode extends VariableNode implements LValueNode {

  public VariableSetNode(String name, Position position) {
    super(name, position);
  }

  /** Accept the LValueNode visitor. */
  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }
}
