package com.plasstech.lang.d2.parse.node;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/** Abstract base class for nodes in the parse tree. */
abstract class AbstractNode implements Node {
  private final Position position;
  private VarType varType = VarType.UNKNOWN;
  private Location location;

  // TODO: take start and end instead of just position.
  AbstractNode(Position position) {
    this.position = position;
  }

  // Indicates it's a simple type - constant
  @Override
  public boolean isConstant() {
    return false;
  }

  @Override
  public VarType varType() {
    return varType;
  }

  @Override
  public void setVarType(VarType varType) {
    Preconditions.checkArgument(!varType.isUnknown(), "Cannot set to unknown");
    internalSetVarType(varType);
  }

  /** This is only used for parameters with no type. */
  protected final void internalSetVarType(VarType varType) {
    Preconditions.checkArgument(
        this.varType.isUnknown(),
        "Cannot overwrite already-set vartype of node "
            + this.toString()
            + ". Was: "
            + this.varType.name());
    Preconditions.checkNotNull(varType, "Cannot set to null");
    this.varType = varType;
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
    this.location = location;
  }

  @Override
  public Location location() {
    return location;
  }
}
