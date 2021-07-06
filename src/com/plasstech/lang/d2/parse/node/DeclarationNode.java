package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/** Declare a variable, e.g., "foo:int" */
public class DeclarationNode extends AbstractNode implements StatementNode {

  private final String variableName;

  public DeclarationNode(String variableName, VarType type, Position position) {
    super(position);
    this.variableName = variableName;
    setVarType(type);
  }

  /** Variable name */
  public String name() {
    return variableName;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("DeclNode: %s: %s", variableName, varType());
  }
}
