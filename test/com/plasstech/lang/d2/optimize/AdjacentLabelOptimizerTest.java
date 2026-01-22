package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.IfOp;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.optimize.testing.OptimizerWithNop;
import com.plasstech.lang.d2.type.VarType;

public class AdjacentLabelOptimizerTest {
  private final Optimizer optimizer = new OptimizerWithNop(new AdjacentLabelOptimizer(2));

  private static final Label LABEL1 = new Label("l1");
  private static final Label LABEL2 = new Label("l2");
  private static final Label LABEL3 = new Label("l3");
  private static final TempLocation TEMP1 = LocationUtils.newTempLocation("temp1", VarType.INT);
  private static final Op OP = new Dec(TEMP1);
  private static final Operand CONDITION = ConstantOperand.TRUE;

  @Test
  public void labelLabelDeletesSecond() {
    ImmutableList<Op> program = ImmutableList.of(LABEL1, LABEL2);

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isEqualTo(LABEL1);
  }

  @Test
  public void labelLabelReplacesGotoSecond() {
    ImmutableList<Op> program = ImmutableList.of(new Goto(LABEL2.label()), LABEL1, LABEL2);

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(2);
    Goto gotoOp = (Goto) optimized.get(0);
    assertThat(gotoOp.label()).isEqualTo(LABEL1.label());
    assertThat(optimized.get(1)).isEqualTo(LABEL1);
  }

  @Test
  public void labelLabelLabelRemovesSecond() {
    ImmutableList<Op> program = ImmutableList.of(new Goto(LABEL3.label()), LABEL1, LABEL2, LABEL3);

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer).isChanged();
    // Run again
    optimized = optimizer.optimize(optimized, null);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(2);
    Goto gotoOp = (Goto) optimized.get(0);
    assertThat(gotoOp.label()).isEqualTo(LABEL1.label());
    assertThat(optimized.get(1)).isEqualTo(LABEL1);
  }

  @Test
  public void labelLabelReplacesIf() {
    ImmutableList<Op> program =
        ImmutableList.of(new IfOp(CONDITION, LABEL2.label(), false), LABEL1, LABEL2);

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(2);
    IfOp ifOp = (IfOp) optimized.get(0);
    assertThat(ifOp.destination()).isEqualTo(LABEL1.label());

    assertThat(optimized.get(1)).isEqualTo(LABEL1);
  }

  @Test
  public void labelLabelReplacesRelevantIfOnly() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new IfOp(CONDITION, LABEL2.label(), false),
            LABEL1,
            LABEL2,
            new IfOp(CONDITION, LABEL3.label(), false));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(3);
    IfOp ifOp = (IfOp) optimized.get(0);
    assertThat(ifOp.destination()).isEqualTo(LABEL1.label());

    assertThat(optimized.get(1)).isEqualTo(LABEL1);

    ifOp = (IfOp) optimized.get(2);
    assertThat(ifOp.destination()).isEqualTo(LABEL3.label());
  }

  @Test
  public void separatedLabelsNotAffected() {
    ImmutableList<Op> program = ImmutableList.of(LABEL1, OP, LABEL2);

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer).isNotChanged();
    assertThat(optimized).isEqualTo(program);
  }
}
