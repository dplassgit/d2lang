package com.plasstech.lang.d2.type;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.Collection;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.parse.node.ArrayDeclarationNode;
import com.plasstech.lang.d2.parse.node.ConstNode;
import com.plasstech.lang.d2.parse.node.DeclarationNode;
import com.plasstech.lang.d2.parse.node.ExprNode;
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

  public class ArrayField extends Field {
    private final VarType baseType;
    private final ImmutableList<Integer> sizes;
    private final ArrayType arrayType;

    public ArrayField(String name, ArrayType arrayType, int offset, List<Integer> sizes) {
      super(name, arrayType, offset);
      this.arrayType = arrayType;
      this.baseType = arrayType.baseType();
      this.sizes = ImmutableList.copyOf(sizes);
    }

    @Override
    public ArrayType type() {
      return arrayType;
    }

    public ImmutableList<Integer> sizes() {
      return sizes;
    }

    public VarType baseType() {
      return baseType;
    }
  }

  private final ImmutableMap<String, Field> fields;
  private final int allocatedSize;

  public RecordSymbol(RecordDeclarationNode node) {
    super(node.name());
    // This isn't *quite* true. It's more of a RecordDefinitionType
    this.setVarType(new RecordReferenceType(node.name()));

    ImmutableMap.Builder<String, Field> fieldBuilder = ImmutableMap.builder();
    int sizeToAllocate = 0;
    for (DeclarationNode decl : node.fields()) {
      Field field;
      if (decl.varType().isArray()) {
        ArrayType arrayType = (ArrayType) decl.varType();
        ArrayDeclarationNode anode = (ArrayDeclarationNode) decl;
        ExprNode size = anode.sizeExpr();
        ConstNode<Integer> constSize = (ConstNode<Integer>) size;
        field =
            new ArrayField(
                decl.name(),
                arrayType,
                sizeToAllocate,
                ImmutableList.of(constSize.value()));
      } else {
        field = new Field(decl.name(), decl.varType(), sizeToAllocate);
      }
      fieldBuilder.put(decl.name(), field);
      sizeToAllocate += decl.varType().size();
    }
    allocatedSize = sizeToAllocate;
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

  public ArrayField getArrayField(String fieldName) {
    Field field = fields.get(fieldName);
    Preconditions.checkNotNull(field, "Cannot find field name %s", fieldName);
    if (field.type.isArray() && field instanceof ArrayField) {
      return (ArrayField) field;
    }
    throw new IllegalStateException("Requested ArrayField for non-array field " + fieldName);
  }

  public ImmutableList<ArrayField> arrayFields() {
    return fields
        .values()
        .stream()
        .filter(f -> f.type().isArray())
        .map(
            f -> {
              return (ArrayField) f;
            })
        .collect(toImmutableList());
  }
}
