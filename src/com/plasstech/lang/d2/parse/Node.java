package com.plasstech.lang.d2.parse;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/**
 * Abstract base class for nodes in the parse tree.
 */
public abstract class Node {
  public enum Type {
    INT, KEYWORD, VARIABLE, PRINT, ERROR, ASSIGNMENT, EXPR, BLOCK, BIN_OP, BOOL, UNARY, IF,
    MAIN, PROC;
  }

  private final Type type;
  private final Position position;
  private VarType varType = VarType.UNKNOWN;

  Node(Type type, Position position) {
    this.type = type;
    this.position = position;
  }

  public Type nodeType() {
    return type;
  }

  public boolean isSimpleType() {
    return type == Type.INT || type == Type.BOOL;
  }

  public VarType varType() {
    return varType;
  }

  public void setVarType(VarType varType) {
    Preconditions.checkArgument(this.varType.isUnknown(),
            "Cannot overwrite already-set vartype. Was: " + this.varType.name());
    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set to unknown");
    Preconditions.checkNotNull(varType, "Cannot set to null");
    this.varType = varType;
  }

  public boolean isError() {
    return type == Type.ERROR;
  }

  public Position position() {
    return position;
  }

  public void accept(NodeVisitor visitor) {
    // do nothing.
  }
}
