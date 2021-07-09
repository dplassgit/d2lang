package com.plasstech.lang.d2.parse.node;

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
    return String.format("NewNode: %s", recordName);
  }
}
