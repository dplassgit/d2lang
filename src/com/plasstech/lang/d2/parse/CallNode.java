package com.plasstech.lang.d2.parse;

import java.util.List;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

/**
 * Represents a node for calling a function/procedure with actual parameters
 */
public class CallNode extends ExprNode {

  private final String functionToCall;
  private final List<ExprNode> actuals;

  CallNode(Position position, String functionToCall, List<ExprNode> actuals) {
    super(position);
    this.functionToCall = functionToCall;
    this.actuals = actuals;
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
    super.accept(visitor);
  }
}