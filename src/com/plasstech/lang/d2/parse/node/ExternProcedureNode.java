package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/** A procedure node that indicates an "extern"ally defined procedure. */
public class ExternProcedureNode extends ProcedureNode {

  public ExternProcedureNode(
      String name, List<Parameter> params, VarType returnType, Position start) {
    super(name, params, returnType, BlockNode.EMPTY, start);
  }

  @Override
  public BlockNode block() {
    throw new IllegalStateException("Should not try to get the block of extern PROC " + name());
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
