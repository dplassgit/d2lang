package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;

public class ConstantPropagationOptimizerTest {
  private Optimizer optimizer =
      new ILOptimizer(ImmutableList.of(new ConstantPropagationOptimizer(2), new IncDecOptimizer(2)))
          .setDebugLevel(2);

  @Test
  public void doubleCopy() {
    TempLocation t1 = new TempLocation("t1");
    TempLocation t2 = new TempLocation("t2");
    StackLocation s1 = new StackLocation("s1");
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(t1, ConstantOperand.ONE),
            new Transfer(t2, t1),
            new Transfer(s1, t2));
    program = optimizer.optimize(program);
    while (optimizer.isChanged()) {
      program = optimizer.optimize(program);
    }

    assertThat(program.get(0)).isInstanceOf(Nop.class);
    assertThat(program.get(1)).isInstanceOf(Nop.class);
    Transfer last = (Transfer)program.get(2);
    assertThat(last.source()).isEqualTo(ConstantOperand.ONE);
  }

  @Test
  public void inc() {
    TestUtils.optimizeAssertSameVariables(
        "      inc:proc(n:int):int {\n"
            + "  sum = 0\n"
            + "  sum = sum + 1\n"
            + "  return sum"
            + "}"
            + "println inc(10)",
        optimizer);
  }

  @Test
  public void dec() {
    TestUtils.optimizeAssertSameVariables(
        "      inc:proc(n:int):int {\n"
            + "  sum = 0\n"
            + "  sum = sum - 1\n"
            + "  return sum"
            + "}"
            + "println inc(10)",
        optimizer);
  }
}
