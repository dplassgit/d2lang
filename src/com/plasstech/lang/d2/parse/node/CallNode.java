package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;
import java.util.List;

/** Represents a node for calling a function/procedure with actual parameters */
public class CallNode extends AbstractNode implements ExprNode, StatementNode {

  private final String functionToCall;
  private final List<ExprNode> actuals;
  private final boolean isStatement;

  public CallNode(
      Position position, String functionToCall, List<ExprNode> actuals, boolean isStatement) {
    super(position);
    this.functionToCall = functionToCall;
    this.actuals = actuals;
    this.isStatement = isStatement;
  }

  public String functionToCall() {
    return functionToCall;
  }

  public List<ExprNode> actuals() {
    return actuals;
  }

  @Override
  public String toString() {
    return String.format("CallNode: %s(%s)", functionToCall, actuals);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  public boolean isStatement() {
    return isStatement;
  }
}
