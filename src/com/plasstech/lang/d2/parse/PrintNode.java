package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

/**
 * Print statement: print [expr]
 */
public class PrintNode extends StatementNode {
  private final Node expr;

  public PrintNode(Node expr, Position position) {
    super(Type.PRINT, position);
    this.expr = expr;
  }

  // May be a binop or atom, etc.
  public Node expr() {
    return expr;
  }

  @Override
  public String toString() {
    return String.format("PrintNode: print %s", expr.toString());
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
