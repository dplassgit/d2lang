package com.plasstech.lang.d2.parse.node;

import java.util.Optional;

import com.plasstech.lang.d2.common.Position;

/** A "while" loop node. */
public class WhileNode extends AbstractNode implements StatementNode {

  private final ExprNode condition;
  private final Optional<StatementNode> doStatement;
  private final BlockNode block;

  public WhileNode(
      ExprNode condition, Optional<StatementNode> doStatement, BlockNode block, Position start) {
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
    if (doStatement.isPresent()) {
      return String.format("WHILE %s DO %s {%s}", condition, doStatement.get(), block);
    } else {
      return String.format("WHILE %s {%s}", condition, block);
    }
  }
}
