package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;

public class ArraySetNode extends AbstractNode implements LValueNode {

  private final String variableName;
  private final ExprNode indexNode;

  public ArraySetNode(String variableName, ExprNode indexNode, Position start) {
    super(start);
    this.variableName = variableName;
    this.indexNode = indexNode;
  }

  @Override
  public String toString() {
    return name();
  }

  @Override
  public String name() {
    return String.format("ArraySetNode: %s[%s]", variableName(), indexNode().toString());
  }

  @Override
  public void accept(Visitor visitor) {
    visitor.visit(this);
  }

  public ExprNode indexNode() {
    return indexNode;
  }

  public String variableName() {
    return variableName;
  }
}
