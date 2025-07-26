package com.plasstech.lang.d2.type;

import static com.google.common.collect.ImmutableList.toImmutableList;

import java.util.Collection;
import java.util.List;
import java.util.Map;

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

  public static class Field {
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

  public static class ArrayField extends Field {
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
  private final ImmutableList<String> unboundTypeVariables;
  private final String baseName;

  private RecordSymbol(String fqName,
      String baseName,
      List<String> formalTypeVariables,
      Map<String, Field> fields,
      int allocatedSize) {
    super(fqName);
    this.baseName = baseName;
    this.unboundTypeVariables = ImmutableList.copyOf(formalTypeVariables);
    this.fields = ImmutableMap.copyOf(fields);
    this.allocatedSize = allocatedSize;
  }

  private RecordSymbol(String baseName,
      ImmutableList<String> formalTypeVariables,
      List<DeclarationNode> declaredFields) {
    super(RecordDeclarationNode.fqName(baseName, formalTypeVariables));
    this.baseName = baseName;
    this.unboundTypeVariables = formalTypeVariables;
    this.setVarType(new RecordReferenceType(baseName,
        formalTypeVariables.stream().map(name -> new UnboundType(name))
            .collect(toImmutableList())));
    ImmutableMap.Builder<String, Field> fieldBuilder = ImmutableMap.builder();
    int sizeToAllocate = 0;
    for (DeclarationNode decl : declaredFields) {
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

    this.allocatedSize = sizeToAllocate;
    this.fields = fieldBuilder.build();
  }

  public RecordSymbol(RecordDeclarationNode node) {
    this(node.baseName(), node.formalTypeVariables(), node.fields());
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

  // This means it's generic - bound or unbound.
  public boolean isGeneric() {
    return !unboundTypeVariables.isEmpty();
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

  /**
   * Returns a new RecordSymbol which is this one with all the generic field types bound to concrete
   * vartypes.
   * 
   * @param mapping from variable type to concrete type.
   * @return
   */
  public RecordSymbol bind(Map<String, VarType> mapping) {
    Preconditions.checkState(isGeneric(),
        "Cannot bind type variables in non-generic or already bound record symbol "
            + this.toString());

    // Make sure all the unbound variables are accounted for in the mapping.
    Preconditions.checkArgument(
        unboundTypeVariables.stream().filter(unboundName -> mapping.containsKey(unboundName))
            .count() == unboundTypeVariables.size());

    // To make the right fq name, we need to go in the *same order* as the unbound names.
    List<VarType> boundTypes = unboundTypeVariables.stream()
        .map(unboundName -> mapping.get(unboundName)).toList();
    String fqName = RecordReferenceType.toFqName(baseName, boundTypes);
    ImmutableMap.Builder<String, Field> newFields = ImmutableMap.builder();
    int newAllocatedSize = 0;
    for (Map.Entry<String, Field> entry : fields.entrySet()) {
      Field field = entry.getValue();
      String name = entry.getKey();
      Field newField = field; // default
      int fieldSize = field.type().size();

      if (field instanceof ArrayField arrayField) {
        // Might have to bind it if it's an array of records
        ArrayType arrayType = arrayField.arrayType;
        if (arrayType.baseType().isRecord()) {
          RecordReferenceType baseType = (RecordReferenceType) arrayType.baseType();
          if (baseType.isGeneric() && !baseType.isBound()) {
            baseType = baseType.bind(mapping);
            arrayType = new ArrayType(baseType, arrayType.dimensions());
            field =
                new ArrayField(
                    field.name,
                    arrayType,
                    newAllocatedSize,
                    arrayField.sizes());
          }
        }
      } else if (field.type instanceof UnboundType unboundType) {
        VarType boundType = mapping.get(unboundType.name());
        if (boundType == null) {
          throw new IllegalStateException("Could not find unbound type " + unboundType.name());
        }
        newField = new Field(name, boundType, newAllocatedSize);
        // force it to 8 bytes, the maximum, so generic method codegen will
        // work even for small types.
        fieldSize = 8;
      } else if (field.type instanceof RecordReferenceType subfield) {
        newField = new Field(name, subfield.bind(mapping), newAllocatedSize);
        fieldSize = 8;
      }

      newFields.put(name, newField);
      newAllocatedSize += fieldSize;
    }

    return new RecordSymbol(fqName, baseName, unboundTypeVariables, newFields.build(),
        newAllocatedSize);

  }

  public ImmutableList<String> formalTypeVariables() {
    return unboundTypeVariables;
  }
}
