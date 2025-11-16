package com.plasstech.lang.d2.parse.node;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.RecordReferenceType;
import com.plasstech.lang.d2.type.VarType;
import java.util.List;

/** This is similar to a UnaryNode, except a little simpler. */
public class NewNode extends AbstractNode implements ExprNode {

  private final String recordName;
  // Actual bound types (for generic formals)
  private final ImmutableList<VarType> actualTypes;
  private final String fqName;

  public NewNode(String recordName, Position position) {
    this(recordName, ImmutableList.of(), position);
  }

  public NewNode(String recordName, List<VarType> actualTypes, Position position) {
    super(position);
    this.recordName = recordName;
    this.fqName = RecordReferenceType.toFqName(recordName, actualTypes);
    this.actualTypes = ImmutableList.copyOf(actualTypes);
    this.setVarType(new RecordReferenceType(recordName, ImmutableList.of(), actualTypes));
  }

  public String baseRecordName() {
    return recordName;
  }

  public String fullyQualifiedRecordName() {
    return fqName;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("NEW %s", recordName);
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null || !(obj instanceof NewNode)) {
      return false;
    }
    return this.hashCode() == obj.hashCode();
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(recordName, varType(), getClass());
  }

  public ImmutableList<VarType> actualTypes() {
    return actualTypes;
  }
}
