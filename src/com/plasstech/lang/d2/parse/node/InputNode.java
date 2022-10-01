package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/** Provides all of standard.in as the result. */
public class InputNode extends AbstractNode implements ExprNode {

  public InputNode(Position position) {
    super(position);
    this.setVarType(VarType.STRING);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return "INPUT";
  }
}
