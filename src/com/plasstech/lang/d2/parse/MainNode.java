package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

public class MainNode extends ProcedureNode {

  public static final MainNode EMPTY = new MainNode(null, null);

  MainNode(BlockNode statements, Position start) {
    super("main", statements, start);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
