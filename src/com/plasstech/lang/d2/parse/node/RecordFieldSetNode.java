package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.common.Position;

public class RecordFieldSetNode extends AbstractNode implements LValueNode {

  private final String variableName;
  private final String fieldName;

  RecordFieldSetNode(String variableName, String fieldName, Position position) {
    super(position);
    this.variableName = variableName;
    this.fieldName = fieldName;
  }

  public String variableName() {
    return variableName;
  }

  public String fieldName() {
    return fieldName;
  }

  @Override
  public String name() {
    return variableName + "." + fieldName;
  }

  // TODO: visit
  // TODO: toString
}
