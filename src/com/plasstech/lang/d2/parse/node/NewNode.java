package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.RecordReferenceType;

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

  // TODO: visit
  // TODO: tostring

}
