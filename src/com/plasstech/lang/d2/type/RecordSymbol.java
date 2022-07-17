package com.plasstech.lang.d2.type;

import java.util.Collection;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.RecordDeclarationNode;

/** Represents a symbol in the symbol table for a record type definition. */
public class RecordSymbol extends AbstractSymbol {

  public class Field {
    private final String name;
    private final VarType type;
    private final int offset;

    public Field(String name, VarType type, int offset) {
      this.name = name;
      this.type = type;
      this.offset = offset;
    }

    public String name() {
      return name;
    }

    public VarType type() {
      return type;
    }

    public int offset() {
      return offset;
    }

    @Override
    public String toString() {
      return String.format("%s: %s (offset %d)", name, type, offset);
    }
  }

  private final ImmutableMap<String, Field> fields;
  private int allocatedSize;

  public RecordSymbol(RecordDeclarationNode node) {
    super(node.name());
    // This isn't *quite* true. It's more of a RecordDefinitionType
    this.setVarType(new RecordReferenceType(node.name()));

    ImmutableMap.Builder<String, Field> fieldBuilder = ImmutableMap.builder();
    allocatedSize = 0;
    for (DeclarationNode decl : node.fields()) {
      fieldBuilder.put(decl.name(), new Field(decl.name(), decl.varType(), allocatedSize));
      allocatedSize += decl.varType().size();
    }
    fields = fieldBuilder.build();
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
    return String.format("Record %s: %s", name(), fields);
  }

  /** In the same order as definition */
  public Collection<String> fieldNames() {
    return fields.keySet();
  }

  /**
   * Given a field name, returns the type of the field. If the field is not found, returns UNKNOWN
   */
  public VarType fieldType(String fieldName) {
    Field field = fields.get(fieldName);
    if (field == null) {
      return VarType.UNKNOWN;
    }
    return field.type;
  }

  public Field getField(String fieldName) {
    return fields.get(fieldName);
  }
}
