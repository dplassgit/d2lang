package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/** Represents a procedure declaration. */
public class ProcedureNode extends DeclarationNode {
  public static class Parameter extends DeclarationNode {
    public Parameter(String name, VarType type, Position position) {
      super(name, type, position);
    }

    /** no type defined */
    public Parameter(String name, Position position) {
      this(name, VarType.UNKNOWN, position);
    }

    @Override
    public String toString() {
      return String.format("%s:%s", name(), varType());
    }
  }

  private final BlockNode block;
  private final ImmutableList<Parameter> parameters;
  private final VarType returnType;

  public ProcedureNode(
      String name, List<Parameter> params, VarType returnType, BlockNode block, Position start) {
    super(name, returnType, start);

    this.parameters = ImmutableList.copyOf(params);
    this.returnType = returnType;
    this.block = (block != null) ? block : BlockNode.EMPTY;
  }

  public ImmutableList<Parameter> parameters() {
    return parameters;
  }

  // We should be allowed to set the return type
  public VarType returnType() {
    return returnType;
  }

  public BlockNode block() {
    return block;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format(
        "ProcedureNode: %s: proc(%s) returns %s:\n{%s\n}", name(), parameters, returnType, block);
  }
}
