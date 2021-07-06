package com.plasstech.lang.d2.type;

import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;

/** Represents a symbol in the symbol table for a record type definition. */
public class RecordSymbol extends AbstractSymbol {

  private final RecordDeclarationNode node;

  public RecordSymbol(RecordDeclarationNode node) {
    super(node.name());
    // This isn't *quite* true. It's more of a RecordDefinitionType
    this.setType(new RecordReferenceType(node.name()));
    this.node = node;
  }

  @Override
  public SymbolStorage storage() {
    return SymbolStorage.GLOBAL;
  }

  public RecordDeclarationNode node() {
    return node;
  }

  @Override
  public String toString() {
    return node.toString();
  }
}
