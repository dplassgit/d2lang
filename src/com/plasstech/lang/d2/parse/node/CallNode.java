package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.plasstech.lang.d2.common.Position;

/** Represents a node for calling a procedure with actual parameters */
public class CallNode extends AbstractNode implements ExprNode, StatementNode {

  private final String procName;
  private final List<ExprNode> actuals;
  private final boolean isStatement;

  public CallNode(Position position, String procName, List<ExprNode> actuals, boolean isStatement) {
    super(position);
    this.procName = procName;
    this.actuals = actuals;
    this.isStatement = isStatement;
  }

  public String procName() {
    return procName;
  }

  public List<ExprNode> actuals() {
    return actuals;
  }

  @Override
  public String toString() {
    return String.format("%s(%s)", procName, actuals);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  /**
   * @return {code}true{/code} if this node is a statement (i.e., not part of an expression). Note
   *     that the proc may return a value but it is going to be ignored.
   */
  public boolean isStatement() {
    return isStatement;
  }
}
