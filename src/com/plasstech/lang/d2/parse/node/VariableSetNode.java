package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;

public class VariableSetNode extends VariableNode implements LValueNode {

  public VariableSetNode(String name, Position position) {
    super(name, position);
  }
}
