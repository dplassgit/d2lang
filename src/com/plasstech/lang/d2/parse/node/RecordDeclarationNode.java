package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.RecordReferenceType;

public class RecordDeclarationNode extends DeclarationNode {

  private final List<DeclarationNode> fields;

  public RecordDeclarationNode(String name, List<DeclarationNode> fields, Position start) {
    super(name, new RecordReferenceType(name), start);
    this.fields = fields;
  }

  public List<DeclarationNode> fields() {
    return fields;
  }

  @Override
  public String toString() {
    return String.format("%s: record {%s}", name(), fields());
  }
}
