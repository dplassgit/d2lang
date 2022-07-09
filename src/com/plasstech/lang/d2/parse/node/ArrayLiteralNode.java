package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.plasstech.lang.d2.common.Position;

public class ArrayLiteralNode extends AbstractNode implements ExprNode {

  private final List<ExprNode> elements;

  public ArrayLiteralNode(Position position, List<ExprNode> elements) {
    super(position);
    this.elements = elements;
  }

  public List<ExprNode> elements() {
    return elements;
  }
}
