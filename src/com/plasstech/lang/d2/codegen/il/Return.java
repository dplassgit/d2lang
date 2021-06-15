package com.plasstech.lang.d2.codegen.il;

import java.util.Optional;

import com.plasstech.lang.d2.codegen.Operand;

public class Return extends Op {

  private final Optional<Operand> returnValueLocation;

  public Return() {
    this.returnValueLocation = Optional.empty();
  }

  public Return(Operand operand) {
    this.returnValueLocation = Optional.of(operand);
  }

  public Optional<Operand> returnValueLocation() {
    return returnValueLocation;
  }

  @Override
  public String toString() {
    return String.format("return %s;",
            returnValueLocation.isPresent() ? returnValueLocation.get().toString() : "");
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
