package com.plasstech.lang.d2.parse.node;

import com.google.common.base.Objects;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.RecordReferenceType;

/** This is similar to a UnaryNode, except a little simpler. */
public class NewNode extends AbstractNode implements ExprNode {

  private final String recordName;

  public NewNode(String recordName, Position position) {
    super(position);
    this.recordName = recordName;
    this.setVarType(new RecordReferenceType(recordName));
  }

  public String recordName() {
    return recordName;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("NEW %s", recordName);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof NewNode)) {
      return false;
    }
    return this.hashCode() == obj.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(recordName, varType(), getClass());
  }
}
