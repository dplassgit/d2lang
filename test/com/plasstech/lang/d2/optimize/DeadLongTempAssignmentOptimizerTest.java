package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.optimize.testing.OptimizerWithNop;
import org.junit.Test;

public class DeadLongTempAssignmentOptimizerTest {
  private final Optimizer optimizer = new OptimizerWithNop(new DeadLongTempAssignmentOptimizer(2));

  private static final Location A = LocationUtils.newParamLocation("a", null, 0, 0);
  private static final Location B = LocationUtils.newParamLocation("b", null, 0, 0);
  private static final Location C = LocationUtils.newParamLocation("c", null, 0, 0);
  private static final Location LONG_TEMP = LocationUtils.newLongTempLocation("longTemp", null);

  @Test
  public void noCode_isUnchanged() {
    ImmutableList<Op> output = optimizer.optimize(ImmutableList.of(), null);
    assertThat(optimizer.isChanged()).isFalse();
    assertThat(output).isEmpty();
  }

  @Test
  public void noLongTemps_isUnchanged() {
    ImmutableList<Op> code = ImmutableList.of(new BinOp(A, B, TokenType.PLUS, C, null));
    ImmutableList<Op> output = optimizer.optimize(code, null);
    assertThat(optimizer.isChanged()).isFalse();
    assertThat(output).isEqualTo(code);
  }

  @Test
  public void longTempUsed_isUnchanged() {
    ImmutableList<Op> code =
        ImmutableList.of(new Transfer(LONG_TEMP, A, null), new Transfer(A, LONG_TEMP, null));

    optimizer.optimize(code, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void longTempNotRead_isRemoved() {
    ImmutableList<Op> code = ImmutableList.of(new Transfer(LONG_TEMP, A, null));

    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).isEmpty();
  }

  @Test
  public void longTempRead_isNotRemoved() {
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(LONG_TEMP, A, null),
            new Transfer(A, LONG_TEMP, null),
            new Transfer(LONG_TEMP, B, null),
            new Transfer(A, B, null));

    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(3);
  }
}
