package com.plasstech.lang.d2.parse.node;

import java.util.Optional;

import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

public class ExitNode extends AbstractNode implements StatementNode {

  private final Optional<ExprNode> message;

  public ExitNode(Position position, ExprNode message) {
    super(position);
    this.message = Optional.of(message);
  }

  public ExitNode(Position position) {
    super(position);
    this.message = Optional.empty();
    this.setVarType(VarType.VOID);
  }

  public Optional<ExprNode> exitMessage() {
    return message;
  }

  @Override
  public void setVarType(VarType varType) {
    super.setVarType(varType);
    // This is weird.
    if (message.isPresent()) {
      if (message.get().varType().isUnknown()) {
        message.get().setVarType(varType);
      }
    }
  }

  @Override
  public String toString() {
    if (message.isPresent()) {
      return String.format("{ExitNode: return %s}", message.get().toString());
    } else {
      return "{ExitNode: return}";
    }
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }
}
