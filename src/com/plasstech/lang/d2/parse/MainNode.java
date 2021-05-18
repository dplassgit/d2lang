package com.plasstech.lang.d2.parse;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

public class MainNode extends ProcedureNode {

  public static final MainNode EMPTY = new MainNode(null, null);

  MainNode(BlockNode statements, Position start) {
    // TODO(issue #8): If user specified args, provide command-line args.
    super("main", ImmutableList.of(), VarType.VOID, statements, start);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
