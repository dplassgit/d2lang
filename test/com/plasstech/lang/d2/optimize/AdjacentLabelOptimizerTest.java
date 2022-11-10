package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.Label;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.VarType;

public class AdjacentLabelOptimizerTest {
  private final Optimizer optimizer = new AdjacentLabelOptimizer(2);
  private final Optimizer OPTIMIZERS =
      new ILOptimizer(ImmutableList.of(optimizer, new NopOptimizer())).setDebugLevel(2);

  private static final Label LABEL1 = new Label("l1");
  private static final Label LABEL2 = new Label("l2");
  private static final Label LABEL3 = new Label("l3");
  private static final TempLocation TEMP1 = new TempLocation("temp1", VarType.INT);
  private static final Op OP = new Dec(TEMP1);

  @Test
  public void labelLabelDeletesSecond() {
    ImmutableList<Op> program = ImmutableList.of(LABEL1, LABEL2);

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isEqualTo(LABEL1);
  }

  @Test
  public void labelLabelReplacesGotoSecond() {
    ImmutableList<Op> program = ImmutableList.of(new Goto(LABEL2.label()), LABEL1, LABEL2);

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    Goto gotoOp = (Goto) optimized.get(0);
    assertThat(gotoOp.label()).isEqualTo(LABEL1.label());
    assertThat(optimized.get(1)).isEqualTo(LABEL1);
  }

  @Test
  public void labelLabelLabelReplacesGotoThird() {
    ImmutableList<Op> program = ImmutableList.of(new Goto(LABEL3.label()), LABEL1, LABEL2, LABEL3);

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    Goto gotoOp = (Goto) optimized.get(0);
    assertThat(gotoOp.label()).isEqualTo(LABEL1.label());
    assertThat(optimized.get(1)).isEqualTo(LABEL1);
  }

  @Test
  public void separatedLabelsNotAffected() {
    ImmutableList<Op> program = ImmutableList.of(LABEL1, OP, LABEL2);

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
    assertThat(optimized).isEqualTo(program);
  }
}
