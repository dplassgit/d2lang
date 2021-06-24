package com.plasstech.lang.d2.parse.node;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;
import java.util.List;

/** Represents a procedure declaration. */
public class ProcedureNode extends AbstractNode implements StatementNode {
  public static class Parameter {
    private final String name;
    private VarType type;

    public Parameter(String name, VarType type) {
      this.name = name;
      this.type = type;
    }

    public Parameter(String name) {
      this(name, VarType.UNKNOWN);
    }

    public String name() {
      return name;
    }

    public VarType type() {
      return type;
    }

    public void setVarType(VarType varType) {
      this.type = varType;
    }

    @Override
    public String toString() {
      return String.format("%s:%s", name, type);
    }
  }

  private final String name;
  private final BlockNode block;
  private final ImmutableList<Parameter> parameters;
  private final VarType returnType;

  public ProcedureNode(
      String name, List<Parameter> params, VarType returnType, BlockNode block, Position start) {
    super(start);

    this.name = name;
    this.parameters = ImmutableList.copyOf(params);
    this.returnType = returnType;
    this.block = (block != null) ? block : BlockNode.EMPTY;
    this.setVarType(returnType); // TODO(Issue #34): Infer return type
  }

  public String name() {
    return name;
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
