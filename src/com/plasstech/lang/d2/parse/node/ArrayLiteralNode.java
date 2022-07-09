package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.VarType;

public class ArrayLiteralNode extends AbstractNode implements ExprNode {

  private final List<ExprNode> elements;
  private final ArrayType arrayType;

  public ArrayLiteralNode(Position position, List<ExprNode> elements, VarType baseType) {
    super(position);
    this.elements = elements;
    this.arrayType = new ArrayType(baseType);
    setVarType(arrayType);
  }

  public List<ExprNode> elements() {
    return elements;
  }

  public ArrayType arrayType() {
    return arrayType;
  }
}
