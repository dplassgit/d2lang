package com.plasstech.lang.d2.parse;

import java.util.Optional;

import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

/** A "while" loop node. */
public class WhileNode extends AbstractNode implements StatementNode {

  private final ExprNode condition;
  private final Optional<StatementNode> doStatement;
  private final BlockNode block;

  public WhileNode(ExprNode condition, Optional<StatementNode> doStatement, BlockNode block,
          Position start) {
    super(start);
    this.condition = condition;
    this.doStatement = doStatement;
    this.block = block;
  }

  public ExprNode condition() {
    return condition;
  }

  public Optional<StatementNode> doStatement() {
    return doStatement;
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
    return String.format("WhileNode: while (%s) do (%s) {%s}", condition, doStatement, block);
  }
}
