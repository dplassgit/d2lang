package com.plasstech.lang.d2.parse.node;

import java.util.List;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

public class ExternProcedureNode extends ProcedureNode {

  public ExternProcedureNode(
      String name, List<Parameter> params, VarType returnType, Position start) {
    super(name, params, returnType, BlockNode.EMPTY, start);
  }
}
