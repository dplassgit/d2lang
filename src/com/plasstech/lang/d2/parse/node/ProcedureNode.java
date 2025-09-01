package com.plasstech.lang.d2.parse.node;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;
import java.util.List;

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
  private final ImmutableList<String> formalTypeVariables;

  public ProcedureNode(
      String name, List<Parameter> params, VarType returnType, BlockNode block, Position start) {
    this(name, params, ImmutableList.of(), returnType, block, start);
  }

  public ProcedureNode(String name, List<Parameter> params, List<String> formalTypeVariables,
      VarType returnType, BlockNode block, Position start) {
    super(name, returnType, start);
    Preconditions.checkArgument(block != null, "`block` cannot be null");
    this.parameters = ImmutableList.copyOf(params);
    this.formalTypeVariables = ImmutableList.copyOf(formalTypeVariables);
    this.returnType = returnType;
    this.block = block;
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
    return String.format("%s: PROC(%s): %s {\n%s\n}", name(), parameters, returnType, block);
  }

  public ImmutableList<String> formalTypeVariables() {
    return formalTypeVariables;
  }
}
