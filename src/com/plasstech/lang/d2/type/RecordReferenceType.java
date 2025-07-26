package com.plasstech.lang.d2.type;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

/** A forward (or backward) reference to a record type. */
public class RecordReferenceType extends PointerType {
  private final ImmutableList<UnboundType> formalTypes;
  private final ImmutableList<VarType> actualTypes;
  private String baseName;
  private String fqName;

  public static String toFqName(String recordName, List<? extends VarType> actualTypes) {
    if (actualTypes.size() > 0) {
      return String.format("%s<%s>", recordName,
          Joiner.on(", ").join(actualTypes.stream().map(VarType::toString).toList()));
    } else {
      return recordName;
    }
  }

  /**
   * @param baseName the base name
   */
  public RecordReferenceType(String baseName) {
    this(baseName, ImmutableList.of(), ImmutableList.of());
  }

  /**
   * @param baseName the base name
   * @param formalTypes the formal types
   */
  public RecordReferenceType(String baseName, List<UnboundType> formalTypes) {
    this(baseName, formalTypes, ImmutableList.of());
  }

  /**
   * @param baseName the base name
   * @param formalTypes the formal types. They may be empty if we don't know yet
   * @param actualTypes the actual types. They may be empty if it's not generic.
   */
  public RecordReferenceType(String baseName, List<UnboundType> formalTypes,
      List<VarType> actualTypes) {
    super(baseName);
    this.formalTypes = ImmutableList.copyOf(formalTypes);
    this.actualTypes = ImmutableList.copyOf(actualTypes);
    if (actualTypes.size() > 0) {
      this.fqName = toFqName(baseName, actualTypes);
    } else if (formalTypes.size() > 0) {
      // I'm not sure about this...
      this.fqName = toFqName(baseName, formalTypes);
    } else {
      this.fqName = baseName;
    }
    this.baseName = baseName;
  }

  public String fqName() {
    return fqName;
  }

  public String baseName() {
    return baseName;
  }

  @Override
  final public boolean isRecord() {
    return true;
  }

  @Override
  final public boolean compatibleWith(VarType that) {
    return that.equals(this) || that.isNull();
  }

  @Override
  public String toString() {
    return String.format("RECORD %s", fqName());
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof RecordReferenceType)) {
      return false;
    }
    return this.hashCode() == obj.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hash(name(), actualTypes(), size()) + 7;
  }

  public ImmutableList<UnboundType> formalTypes() {
    return formalTypes;
  }

  public ImmutableList<VarType> actualTypes() {
    return actualTypes;
  }

  public boolean isGeneric() {
    return !actualTypes.isEmpty() || !formalTypes.isEmpty();
  }

  public boolean isBound() {
    return !actualTypes.isEmpty();
  }

  public RecordReferenceType bind(Map<String, VarType> mapping) {
    Preconditions.checkState(actualTypes.isEmpty(),
        "Cannot re-bind an already bound RECORD %s", toString());
    Preconditions.checkState(!formalTypes.isEmpty(),
        "Cannot bind a non-generic RECORD %s", toString());
    Preconditions.checkState(mapping.size() == formalTypes.size(),
        "Wrong number of actual type parameters to RECORD %s; expected %s, saw %s",
        toString(), formalTypes.size(), mapping.size());
    List<VarType> actuals =
        formalTypes.stream().map(unboundType -> mapping.get(unboundType.name())).toList();
    return new RecordReferenceType(baseName(), formalTypes, actuals);
  }
}
