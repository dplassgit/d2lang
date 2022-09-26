package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.testing.TestUtils;
import com.plasstech.lang.d2.type.VarType;

public class DeadCodeOptimizerTest {
  private Optimizer optimizer =
      new ILOptimizer(
              ImmutableList.of(new ConstantPropagationOptimizer(2), new DeadCodeOptimizer(2)))
          .setDebugLevel(2);
  private static final TempLocation TEMP1 = new TempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = new TempLocation("temp2", VarType.INT);

  @Test
  public void oneLoopBreak() {
    TestUtils.optimizeAssertSameVariables(
        "      oneLoopBreakDCO:proc(n:int):int {\n"
            + "  sum = 0\n"
            + "  i = 0 "
            + "  while i < 10 do i = i + 1 {"
            + "    sum = sum + 1\n"
            + "    break"
            + "  }"
            + "  return sum"
            + "}"
            + "println oneLoopBreakDCO(10)",
        optimizer);
  }

  @Test
  public void incDec() {
    ImmutableList<Op> program = ImmutableList.of(new Inc(TEMP1), new Dec(TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
  }

  @Test
  public void incDecDifferent() {
    ImmutableList<Op> program = ImmutableList.of(new Inc(TEMP1), new Dec(TEMP2));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
    assertThat(optimized.get(0)).isInstanceOf(Inc.class);
    assertThat(optimized.get(1)).isInstanceOf(Dec.class);
  }

  @Test
  public void decInc() {
    ImmutableList<Op> program = ImmutableList.of(new Dec(TEMP1), new Inc(TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
  }

  @Test
  public void decIncDec() {
    ImmutableList<Op> program = ImmutableList.of(new Dec(TEMP1), new Inc(TEMP1), new Dec(TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Dec.class);
  }

  @Test
  public void returnThenStuffThenLabel() {
    ImmutableList<Op> program =
        ImmutableList.of(new Return("name"), new Dec(TEMP1), new Label("label"));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Return.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Label.class);
  }

  @Test
  public void returnThenStuffThenProcEnd() {
    ImmutableList<Op> program =
        ImmutableList.of(new Return("name"), new Dec(TEMP1), new ProcExit("proc", 0));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Return.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(ProcExit.class);
  }

  @Test
  public void returnThenStuffThenGoto() {
    ImmutableList<Op> program =
        ImmutableList.of(new Return("name"), new Dec(TEMP1), new Goto("label"));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Return.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Nop.class);
  }

  @Test
  public void stopThenStuffThenLabel() {
    ImmutableList<Op> program = ImmutableList.of(new Stop(), new Dec(TEMP1), new Label("label"));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Stop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Label.class);
  }

  @Test
  public void stopThenStuffThenGoto() {
    ImmutableList<Op> program = ImmutableList.of(new Stop(), new Dec(TEMP1), new Goto("label"));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Stop.class);
    assertThat(optimized.get(1)).isInstanceOf(Nop.class);
    assertThat(optimized.get(2)).isInstanceOf(Nop.class);
  }

  @Test
  public void ifTrueEqualsGoto() {
    ImmutableList<Op> program = ImmutableList.of(new IfOp(ConstantOperand.of(true), "dest", false));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Goto.class);
  }

  @Test
  public void ifOneEqualsGoto() {
    ImmutableList<Op> program = ImmutableList.of(new IfOp(ConstantOperand.ONE, "dest", false));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Goto.class);
  }

  @Test
  public void ifFalseEqualsNop() {
    ImmutableList<Op> program =
        ImmutableList.of(new IfOp(ConstantOperand.of(false), "dest", false));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
  }

  @Test
  public void ifNotTrueEqualsNop() {
    ImmutableList<Op> program = ImmutableList.of(new IfOp(ConstantOperand.of(true), "dest", true));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Nop.class);
  }

  @Test
  public void ifNotFalseEqualsGoto() {
    ImmutableList<Op> program = ImmutableList.of(new IfOp(ConstantOperand.of(false), "dest", true));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Goto.class);
  }
}
