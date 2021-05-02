package com.plasstech.lang.d2.parse;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.type.VarType;

/**
 * Abstract base class for nodes in the parse tree.
 */
public abstract class Node {
  public enum Type {
    INT, KEYWORD, VARIABLE, PRINT, ERROR, ASSIGNMENT, EXPR, STATEMENTS, BIN_OP;
  }

  private final Type type;
  private VarType varType = VarType.UNKNOWN;

  Node(Type type) {
    this.type = type;
  }

  public Type nodeType() {
    return type;
  }

  public VarType varType() {
    return varType;
  }

  public void setVarType(VarType varType) {
    Preconditions.checkArgument(this.varType == VarType.UNKNOWN,
            "Cannot overwrite already-set vartype. Was: " + this.varType.name());
    this.varType = varType;
  }

  public boolean isError() {
    return type == Type.ERROR;
  }

  public void visit(NodeVisitor visitor) {
    // do nothing.
  }
}
