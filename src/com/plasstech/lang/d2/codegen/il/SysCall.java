package com.plasstech.lang.d2.codegen.il;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;

public class SysCall extends Op {

  public enum Call {
    MESSAGE,
    PARAMETERIZED_MESSAGE,
    PRINT,
    PRINTLN,
    INPUT
  }

  private final Call call;
  private final List<Operand> operands;
  private int line;
  private int column;

  public SysCall(Call call, Operand arg) {
    this(call, ImmutableList.of(arg));
  }

  public SysCall(String parameterizedMessage) {
    this(parameterizedMessage, ImmutableList.of());
  }

  public SysCall(String parameterizedMessage, List<Operand> operands) {
    this(Call.PARAMETERIZED_MESSAGE,
        ImmutableList.<Operand>builder().add(ConstantOperand.of(parameterizedMessage))
            .addAll(operands).build());
  }

  public SysCall(Call call, List<Operand> operands) {
    this.call = call;
    this.operands = ImmutableList.copyOf(operands);
  }

  public Call call() {
    return call;
  }

  public Operand arg() {
    return operands.get(0);
  }

  public int line() {
    Preconditions.checkState(call == Call.PARAMETERIZED_MESSAGE);
    return line;
  }

  public int column() {
    Preconditions.checkState(call == Call.PARAMETERIZED_MESSAGE);
    return column;
  }

  public List<Operand> operands() {
    return operands;
  }

  @Override
  public String toString() {
    switch (call) {
      case PRINT:
        return String.format("printf(\"%%s\", %s)", ESCAPER.escape(arg().toString()));

      case PRINTLN:
        return String.format("printf(\"%%s\\n\", %s)", ESCAPER.escape(arg().toString()));

      case MESSAGE:
        return String.format("printf(\"ERROR: %%s\", %s)", ESCAPER.escape(arg().toString()));

      case PARAMETERIZED_MESSAGE:
        return String.format(
            "printf(\"ERROR: %s, %d, %d, %s)", ESCAPER.escape(arg().toString()), line, column,
            Joiner.on(",").join(operands().subList(1, operands.size())));

      case INPUT:
        return String.format("%s=_read()", arg());

      default:
        throw new IllegalArgumentException("Unknown syscall: " + call);
    }
  }

  @Override
  public void accept(OpcodeVisitor visitor) {
    visitor.visit(this);
  }
}
