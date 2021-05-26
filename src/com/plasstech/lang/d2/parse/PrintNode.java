package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

/**
 * Print statement: print [expr] or println [expr]
 */
public class PrintNode extends AbstractNode implements StatementNode {
  private final ExprNode expr;
  private final boolean println;

  PrintNode(ExprNode expr, Position position, boolean println) {
    super(position);
    this.expr = expr;
    this.println = println;
  }

  // May be a binop or atom, etc.
  public ExprNode expr() {
    return expr;
  }

  public boolean isPrintln() {
    return println;
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
