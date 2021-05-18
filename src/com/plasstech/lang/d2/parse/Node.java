package com.plasstech.lang.d2.parse;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.codegen.il.Location;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/**
 * Abstract base class for nodes in the parse tree.
 */
public abstract class Node {
  private final Position position;
  private VarType varType = VarType.UNKNOWN;
  private Location location;

  Node(Position position) {
    this.position = position;
  }

  // Indicates it's a simple type - constant or variable.
  public boolean isSimpleType() {
    return false;
  }

  public VarType varType() {
    return varType;
  }

  public void setVarType(VarType varType) {
    Preconditions.checkArgument(this.varType.isUnknown(),
            "Cannot overwrite already-set vartype. Was: " + this.varType.name());
    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set to unknown");
    Preconditions.checkNotNull(varType, "Cannot set to null");
    this.varType = varType;
  }

  public boolean isError() {
    return false;
  }

  public Position position() {
    return position;
  }

  public void accept(NodeVisitor visitor) {
    // do nothing.
  }

  public void setLocation(Location location) {
    Preconditions.checkState(this.location == null, "Location cannot be set again");
    this.location = location;
  }

  public Location location() {
    return location;
  }
}
