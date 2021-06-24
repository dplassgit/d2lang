package com.plasstech.lang.d2.parse.node;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.common.Position;
import java.util.List;

/** If/elif/else node. */
public class IfNode extends AbstractNode implements StatementNode {
  public static class Case {
    private final Node condition;
    private final BlockNode block;

    public Case(Node condition, BlockNode block) {
      this.condition = condition;
      this.block = block;
    }

    @Override
    public String toString() {
      return String.format("\nif (%s) {%s}", condition, block);
    }

    public Node condition() {
      return condition;
    }

    public BlockNode block() {
      return block;
    }
  }

  private final List<Case> cases;
  private final BlockNode elseBlock;

  public IfNode(List<Case> cases, BlockNode elseBlock, Position position) {
    super(position);
    Preconditions.checkArgument(cases != null, "cases cannot be null");
    this.cases = cases;
    this.elseBlock = elseBlock;
  }

  public List<Case> cases() {
    return cases;
  }

  public BlockNode elseBlock() {
    return elseBlock;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    if (elseBlock != null) {
      return String.format("IfNode: (%s) else {%s}", cases(), elseBlock());
    } else {
      // this isn't ideal, but shrug.
      return String.format("IfNode: (%s)", cases());
    }
  }
}
