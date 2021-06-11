package com.plasstech.lang.d2.parse.node;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/** Represents the main procedure of a program */
public class MainNode extends ProcedureNode {

  public static final MainNode EMPTY = new MainNode(BlockNode.EMPTY, null);

  public MainNode(BlockNode statements, Position start) {
    // TODO(issue #8): If user specified args, provide command-line args.
    super("main", ImmutableList.of(), VarType.VOID, statements, start);
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}