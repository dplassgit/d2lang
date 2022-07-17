package com.plasstech.lang.d2.type;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.List;

import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;

/** Represents a symbol in the symbol table for a record type definition. */
public class RecordSymbol extends AbstractSymbol {

  private final RecordDeclarationNode node;
  private int allocatedSize;

  public RecordSymbol(RecordDeclarationNode node) {
    super(node.name());
    // This isn't *quite* true. It's more of a RecordDefinitionType
    this.setVarType(new RecordReferenceType(node.name()));
    this.node = node;
    allocatedSize = 0;
    for (DeclarationNode field : node.fields()) {
      allocatedSize += field.varType().size();
    }
  }

  public int allocatedSize() {
    // the size of all fields.
    return allocatedSize;
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.GLOBAL;
  }

  @Override
  public String toString() {
    return String.format("Record %s: %s", node.name(), node.fields());
  }

  /** In the same order as definition */
  public List<String> fieldNames() {
    return node.fields().stream().map(DeclarationNode::name).collect(toImmutableList());
  }

  /**
   * Given a field name, returns the type of the field. If the field is not found, returns UNKNOWN
   */
  public VarType fieldType(String fieldName) {
    for (DeclarationNode field : node.fields()) {
      if (field.name().equals(fieldName)) {
        return field.varType();
      }
    }
    return VarType.UNKNOWN;
  }
}
