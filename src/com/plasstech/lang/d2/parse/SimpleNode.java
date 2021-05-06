package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

public abstract class SimpleNode extends Node {

  SimpleNode(Type type, Position position) {
    super(type, position);
  }

  public abstract String simpleValue();
}
