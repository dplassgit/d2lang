package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

/**
 * Represents a "simple" (i.e., constant or variable) node.
 */
public abstract class SimpleNode extends AbstractNode implements ExprNode {

  SimpleNode(Position position) {
    super(position);
  }

  public abstract String simpleValue();
}
