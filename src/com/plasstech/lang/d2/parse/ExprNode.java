package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

/**
 * Abstract base class for a node that can be an expression.
 */
public abstract class ExprNode extends Node {
  ExprNode(Position position) {
    super(position);
  }
}
