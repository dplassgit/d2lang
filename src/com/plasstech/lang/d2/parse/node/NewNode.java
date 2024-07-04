package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.RecordReferenceType;
import com.plasstech.lang.d2.type.VarType;

/** This is similar to a UnaryNode, except a little simpler. */
public class NewNode extends AbstractNode implements ExprNode {

  private final String recordName;
  private final ImmutableList<VarType> actualTypes;

  public NewNode(String recordName, Position position) {
    this(recordName, ImmutableList.of(), position);
  }

  public NewNode(String recordName, List<VarType> actualTypes, Position position) {
    super(position);
    this.recordName = recordName;
    this.actualTypes = ImmutableList.copyOf(actualTypes);
    this.setVarType(new RecordReferenceType(recordName, actualTypes));
  }

  public String recordName() {
    return recordName;
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
