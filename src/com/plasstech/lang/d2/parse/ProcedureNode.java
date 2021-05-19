package com.plasstech.lang.d2.parse;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.VarType;

public class ProcedureNode extends AbstractNode implements StatementNode {
  public static class Parameter {
    private final String name;
    private final VarType type;

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

    @Override
    public String toString() {
      return String.format("FormalParam: %s:%s", name, type);
    }
  }

  private final String name;
  private final BlockNode block;
  private final ImmutableList<Parameter> parameters;
  private final VarType returnType;
  private SymTab symTab;

  ProcedureNode(String name, List<Parameter> params, VarType returnType, BlockNode block,
          Position start) {
    super(start);

    this.name = name; // TODO: mangle?
    this.parameters = ImmutableList.copyOf(params);
    this.returnType = returnType;
    this.block = block;
    this.setVarType(returnType); // is this required? couldn't we just "figure it out"?
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

  public void setSymbolTable(SymTab symTab) {
    this.symTab = symTab;
  }

  public SymTab symbolTable() {
    return symTab;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("ProcedureNode: %s: proc(%s) returns %s: {%s}", name(), parameters,
            returnType, block);
  }

}
