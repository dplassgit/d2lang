package com.plasstech.lang.d2.codegen.il;

import java.util.Optional;

import com.plasstech.lang.d2.codegen.Operand;

public class Return extends Op {

  private final Optional<Operand> returnValueLocation;
  private final String procName;

  public Return(String procName) {
    this.procName = procName;
    this.returnValueLocation = Optional.empty();
  }

  public Return(String procName, Operand operand) {
    this.procName = procName;
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

  public String procName() {
    return procName;
  }
}
