package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;

/**
 * Print statement: print [expr]
 */
public class PrintNode extends StatementNode {
  private final Node expr;

  public PrintNode(Node expr) {
    super(Type.PRINT);
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
  public void visit(NodeVisitor visitor) {
    visitor.accept(this);
  }
}
