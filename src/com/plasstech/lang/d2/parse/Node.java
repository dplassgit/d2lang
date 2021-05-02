package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;

public abstract class Node {
  // TODO: augment with type of expression

  public enum Type {
    INT, KEYWORD, VARIABLE, PRINT, ERROR, ASSIGNMENT, EXPR, STATEMENTS, BIN_OP;
  }

  private final Type type;

  Node(Type type) {
    this.type = type;
  }

  public Type nodeType() {
    return type;
  }

  public boolean isError() {
    return type == Type.ERROR;
  }

  public void visit(NodeVisitor visitor) {
    // do nothing.
  }
}
