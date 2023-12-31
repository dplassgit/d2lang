package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.OpcodeSubject.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class TempPropagationOptimizerTest {
  private static final Location PARAM = LocationUtils.newParamLocation("param", VarType.INT, 0, 0);
  private static final Location LOCAL = LocationUtils.newStackLocation("stack", VarType.INT, 0);
  private static final Location TEMP1 = LocationUtils.newTempLocation("temp1", VarType.INT);
  private static final Location TEMP2 = LocationUtils.newTempLocation("temp2", VarType.INT);
  private static final Location TEMP3 = LocationUtils.newTempLocation("temp3", VarType.INT);
  private static final Location DPARAM =
      LocationUtils.newParamLocation("dparam", VarType.DOUBLE, 0, 0);
  private static final Location DLOCAL =
      LocationUtils.newStackLocation("dstack", VarType.DOUBLE, 0);
  private static final Location DTEMP1 = LocationUtils.newTempLocation("dtemp1", VarType.DOUBLE);
  private static final Location DTEMP2 = LocationUtils.newTempLocation("dtemp2", VarType.DOUBLE);
  private static final Location DTEMP3 = LocationUtils.newTempLocation("dtemp3", VarType.DOUBLE);

  private Optimizer optimizer =
      new ILOptimizer(ImmutableList.of(new TempPropagationOptimizer(2), new NopOptimizer()));

  @Test
  public void inc_noOptimization() {
    optimizer.optimize(ImmutableList.of(new Inc(PARAM)), null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void noTransfer_noOptimization() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP3, TEMP1, TokenType.PLUS, TEMP2, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void stack_noOptimization() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP3, TEMP1, TokenType.PLUS, TEMP2, null),
            new Transfer(LOCAL, TEMP3, null));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void stack_addLocal_success() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP3, TEMP1, TokenType.PLUS, ConstantOperand.of(3), null),
            new Transfer(LOCAL, TEMP3, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(LOCAL, TEMP1, TokenType.PLUS, ConstantOperand.of(3));
  }

  @Test
  public void success() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP3, TEMP1, TokenType.PLUS, TEMP2, null),
            new Transfer(PARAM, TEMP3, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(PARAM, TEMP1, TokenType.PLUS, TEMP2);
  }

  @Test
  public void doubleConstantOptimized() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DTEMP2, DTEMP1, TokenType.PLUS, ConstantOperand.of(1.0), null),
            new Transfer(DPARAM, DTEMP2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(DPARAM, DTEMP1, TokenType.PLUS, ConstantOperand.of(1.0));
  }

  @Test
  public void doubleOptimized() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DTEMP3, DTEMP1, TokenType.PLUS, DTEMP2, null),
            new Transfer(DPARAM, DTEMP3, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(DPARAM, DTEMP1, TokenType.PLUS, DTEMP2);
  }

  @Test
  public void doubleLocal_notOptimized() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DTEMP2, DTEMP1, TokenType.PLUS, ConstantOperand.of(1.0), null),
            new Transfer(DLOCAL, DTEMP2, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void unaryOp_success() {
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(TEMP3, TokenType.MINUS, TEMP2, null),
            new Transfer(PARAM, TEMP3, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);

    UnaryOp op = (UnaryOp) optimized.get(0);
    assertThat(op.destination()).isEqualTo(PARAM);
    assertThat(op.operator()).isEqualTo(TokenType.MINUS);
    assertThat(op.operand()).isEqualTo(TEMP2);
  }
}
