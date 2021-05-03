package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.Position;

public abstract class StatementNode extends Node {
  StatementNode(Type type, Position position) {
    super(type, position);
  }
}
