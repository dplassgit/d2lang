package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OpcodeSubject.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThatInterpreting;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.optimize.testing.OptimizerWithNop;
import com.plasstech.lang.d2.type.VarType;

public class DeadCodeOptimizerTest {
  private final Optimizer optimizer = new OptimizerWithNop(new DeadCodeOptimizer(2));

  private static final Label LABEL = new Label("label");
  private static final TempLocation TEMP1 = LocationUtils.newTempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = LocationUtils.newTempLocation("temp2", VarType.INT);

  @Test
  public void oneLoopBreak() {
    assertThatInterpreting("""
        oneLoopBreakDCO:proc(n:int):int {
          sum = 0
          i = 0
          while i < 10 do i = i + 1 {
            sum = sum + 1
            break
          }
          return sum
        }
        println oneLoopBreakDCO(10)
        """).withOptimizer(optimizer).hasSameVariables();
  }

  @Test
  public void incDec() {
    ImmutableList<Op> program = ImmutableList.of(new Inc(TEMP1), new Dec(TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).isEmpty();
  }

  @Test
  public void incDecDifferent() {
    ImmutableList<Op> program = ImmutableList.of(new Inc(TEMP1), new Dec(TEMP2));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void decInc() {
    ImmutableList<Op> program = ImmutableList.of(new Dec(TEMP1), new Inc(TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).isEmpty();
  }

  @Test
  public void decIncDec() {
    ImmutableList<Op> program = ImmutableList.of(new Dec(TEMP1), new Inc(TEMP1), new Dec(TEMP1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isInstanceOf(Dec.class);
  }

  @Test
  public void returnThenStuffThenLabel() {
    ImmutableList<Op> program =
        ImmutableList.of(new Return("name"), new Dec(TEMP1), new Label("label"));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isInstanceOf(Return.class);
    assertThat(optimized.get(1)).isInstanceOf(Label.class);
  }

  @Test
  public void returnThenStuffThenProcEnd() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Return("name"), new Dec(TEMP1),
            new ProcExit("proc", 0, 0));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    assertThat(optimized.get(0)).isInstanceOf(Return.class);
    assertThat(optimized.get(1)).isInstanceOf(ProcExit.class);
  }

  @Test
  public void returnThenStuffThenGoto() {
    ImmutableList<Op> program =
        ImmutableList.of(new Return("name"), new Dec(TEMP1), new Goto("label"));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isInstanceOf(Return.class);
  }

  @Test
  public void stopThenStuffThenLabel() {
    ImmutableList<Op> program = ImmutableList.of(new Stop(), new Dec(TEMP1), new Label("label"));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    assertThat(optimized.get(0)).isInstanceOf(Stop.class);
    assertThat(optimized.get(1)).isInstanceOf(Label.class);
  }

  @Test
  public void stopThenStuffThenGoto() {
    ImmutableList<Op> program = ImmutableList.of(new Stop(), new Dec(TEMP1), new Goto("label"));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isInstanceOf(Stop.class);
  }

  @Test
  public void ifTrueEqualsGoto() {
    ImmutableList<Op> program = ImmutableList.of(new IfOp(ConstantOperand.of(true), "dest", false));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isGoto("dest");
  }

  @Test
  public void ifOneEqualsGoto() {
    ImmutableList<Op> program = ImmutableList.of(new IfOp(ConstantOperand.ONE, "dest", false));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isGoto("dest");
  }

  @Test
  public void ifFalseEqualsNop() {
    ImmutableList<Op> program =
        ImmutableList.of(new IfOp(ConstantOperand.of(false), "dest", false));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).isEmpty();
  }

  @Test
  public void ifNotTrueEqualsNop() {
    ImmutableList<Op> program = ImmutableList.of(new IfOp(ConstantOperand.of(true), "dest", true));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).isEmpty();
  }

  @Test
  public void ifNotFalseEqualsGoto() {
    ImmutableList<Op> program = ImmutableList.of(new IfOp(ConstantOperand.of(false), "dest", true));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isGoto("dest");
  }

  @Test
  public void ifThenLabel() {
    ImmutableList<Op> program = ImmutableList.of(new IfOp(TEMP1, LABEL.label(), false), LABEL);

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isEqualTo(LABEL);
  }

  @Test
  public void ifThenDifferentLabel() {
    ImmutableList<Op> program = ImmutableList.of(new IfOp(TEMP1, "dest", false), LABEL);

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
    assertThat(optimized).isEqualTo(program);
  }

  @Test
  public void transferToItself() {
    ImmutableList<Op> program = ImmutableList.of(new Transfer(TEMP1, TEMP1, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).isEmpty();
  }
}
