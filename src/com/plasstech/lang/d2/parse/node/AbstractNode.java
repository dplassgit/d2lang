package com.plasstech.lang.d2.parse.node;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/**
 * Abstract base class for nodes in the parse tree.
 */
abstract class AbstractNode implements Node {
  private final Position position;
  private VarType varType = VarType.UNKNOWN;
  private Location location;

  AbstractNode(Position position) {
    this.position = position;
  }

  // Indicates it's a simple type - constant or variable.
  @Override
  public boolean isSimpleType() {
    return false;
  }

  @Override
  public VarType varType() {
    return varType;
  }

  @Override
  public void setVarType(VarType varType) {
    Preconditions.checkArgument(this.varType.isUnknown(),
            "Cannot overwrite already-set vartype. Was: " + this.varType.name());
    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set to unknown");
    Preconditions.checkNotNull(varType, "Cannot set to null");
    this.varType = varType;
  }

  @Override
  public boolean isError() {
    return false;
  }

  @Override
  public String message() {
    throw new IllegalStateException("No error for non-error node");
  }

  @Override
  public Position position() {
    return position;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    // do nothing.
  }

  @Override
  public void setLocation(Location location) {
    Preconditions.checkState(this.location == null, "Location cannot be set again");
    this.location = location;
  }

  @Override
  public Location location() {
    return location;
  }
}
