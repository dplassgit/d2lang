package com.plasstech.lang.d2.parse;

import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.lex.Token;
import com.plasstech.lang.d2.type.VarType;

/**
 * Binary operation: leftexpr <operation> rightexpr
 */
public class BinOpNode extends Node {
  private static final Set<
          Token.Type> BINARY_OPERATORS = ImmutableSet.of(Token.Type.PLUS, Token.Type.MINUS);

  private final Token.Type opType;
  private final Node left;
  private final Node right;

  BinOpNode(Node left, Token.Type opType, Node right) {
    super(Node.Type.BIN_OP);
    Preconditions.checkArgument(BINARY_OPERATORS.contains(opType),
            "Invalid opType " + opType.name());
    this.left = left;
    this.opType = opType;
    this.right = right;
  }

  public Token.Type opType() {
    return opType;
  }

  public Node left() {
    return left;
  }

  public Node right() {
    return right;
  }

  @Override
  public void setVarType(VarType varType) {
    super.setVarType(varType);
    // This is weird.
    if (left.varType().isUnknown()) {
      left.setVarType(varType);
    }
    if (right.varType().isUnknown()) {
      right.setVarType(varType);
    }
  }

  @Override
  public String toString() {
    return String.format("{BinOpNode: %s %s %s}", left.toString(), opType.name(), right.toString());
  }

  @Override
  public void visit(NodeVisitor visitor) {
    visitor.accept(this);
  }
}
