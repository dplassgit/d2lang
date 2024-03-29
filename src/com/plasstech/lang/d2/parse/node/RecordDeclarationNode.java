package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.RecordReferenceType;

/** A parse node for a record definition declaration. */
public class RecordDeclarationNode extends DeclarationNode {

  private final List<DeclarationNode> fields;

  /**
   * @param name the name of the record
   * @param fields
   * @param start
   */
  public RecordDeclarationNode(String name, List<DeclarationNode> fields, Position start) {
    // Technically this node doesn't have a type because it's not a referenceable *variable*
    super(name, new RecordReferenceType(name), start);
    this.fields = fields;
  }

  /** The fields declared in this record definition. */
  public List<DeclarationNode> fields() {
    return fields;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("%s: RECORD {%s}", name(), fields());
  }
}
