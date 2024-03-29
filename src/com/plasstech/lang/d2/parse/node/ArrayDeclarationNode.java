package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.ArrayType;

/** A declaration of an array, with a dynamic size. */
public class ArrayDeclarationNode extends DeclarationNode {

  private final ExprNode sizeExpr;
  private final ArrayType arrayType;

  // TODO(#38): support multidimensonal arrays
  public ArrayDeclarationNode(
      String varName, ArrayType type, Position position, ExprNode sizeExpr) {
    super(varName, type, position);
    this.arrayType = type;
    this.sizeExpr = sizeExpr;
  }

  public ExprNode sizeExpr() {
    return sizeExpr;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  public ArrayType arrayType() {
    return arrayType;
  }

  @Override
  public String toString() {
    return String.format("%s: %s[%s]", name(), arrayType(), sizeExpr());
  }
}
