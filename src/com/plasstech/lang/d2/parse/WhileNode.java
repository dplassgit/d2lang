package com.plasstech.lang.d2.parse;

import java.util.Optional;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

/** A "while" loop node. */
public class WhileNode extends AbstractNode implements StatementNode {

  private final ExprNode condition;
  private final Optional<AssignmentNode> assignment;
  private final BlockNode block;

  public WhileNode(ExprNode condition, Optional<AssignmentNode> assignment, BlockNode block,
          Position start) {
    super(start);
    this.condition = condition;
    this.assignment = assignment;
    this.block = block;
  }

  public ExprNode condition() {
    return condition;
  }

  public Optional<AssignmentNode> assignment() {
    return assignment;
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
    return String.format("WhileNode: while (%s) do (%s) {%s}", condition, assignment, block);
  }
}
