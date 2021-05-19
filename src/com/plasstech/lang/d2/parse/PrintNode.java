package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

/**
 * Print statement: print [expr]
 */
public class PrintNode extends AbstractNode implements StatementNode {
  private final Node expr;
  private final boolean println;

  public PrintNode(Node expr, Position position, boolean println) {
    super(position);
    this.expr = expr;
    this.println = println;
  }

  // May be a binop or atom, etc.
  public Node expr() {
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
