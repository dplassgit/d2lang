package com.plasstech.lang.d2.parse;

import java.util.List;

import com.google.common.base.Preconditions;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;

public class IfNode extends StatementNode {
  public static class Case {
    private final Node condition;
    private final List<Node> statements;

    public Case(Node condition, List<Node> statements) {
      this.condition = condition;
      this.statements = statements;
    }

    @Override
    public String toString() {
      return String.format("\nIfCaseNode: if (%s)\n{%s}", condition(), statements());
    }

    public Node condition() {
      return condition;
    }

    public List<Node> statements() {
      return statements;
    }
  }

  private final List<Case> cases;
  private final List<Node> elseBlock;

  IfNode(List<Case> cases, List<Node> elseStatements, Position position) {
    super(Type.IF, position);
    Preconditions.checkArgument(cases != null, "cases cannot be null");
    Preconditions.checkArgument(elseStatements != null, "elseStatements cannot be null");
    this.cases = cases;
    this.elseBlock = elseStatements;
  }

  public List<Case> cases() {
    return cases;
  }

  public List<Node> elseBlock() {
    return elseBlock;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    if (!elseBlock().isEmpty()) {
      return String.format("IfNode:\n(%s)\nelse {\n%s}", cases(), elseBlock());
    } else {
      // this isn't ideal, but shrug.
      return String.format("IfNode:\n(%s)", cases());
    }
  }
}
