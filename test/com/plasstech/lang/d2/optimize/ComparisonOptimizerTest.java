package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class ComparisonOptimizerTest {
  private Optimizer optimizer = new ComparisonOptimizer(2);

  private static final TempLocation TEMP1 = LocationUtils.newTempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = LocationUtils.newTempLocation("temp2", VarType.INT);

  @Test
  public void plus() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP2, TEMP1, TokenType.PLUS, ConstantOperand.ONE, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void gtRightConstant() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP2, TEMP1, TokenType.GT, ConstantOperand.ONE, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void gtLeftConstant() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP2, ConstantOperand.ONE, TokenType.GT, TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP2);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.LT);
    assertThat(first.right()).isEqualTo(ConstantOperand.ONE);
  }

  @Test
  public void ltLeftConstant() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP2, ConstantOperand.ONE, TokenType.LT, TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP2);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.GT);
    assertThat(first.right()).isEqualTo(ConstantOperand.ONE);
  }

  @Test
  public void lteLeftConstant() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP2, ConstantOperand.ONE, TokenType.LEQ, TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP2);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.GEQ);
    assertThat(first.right()).isEqualTo(ConstantOperand.ONE);
  }

  @Test
  public void eqLeftConstant() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP2, ConstantOperand.ONE, TokenType.EQEQ, TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.destination()).isEqualTo(TEMP2);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.EQEQ);
    assertThat(first.right()).isEqualTo(ConstantOperand.ONE);
  }

  @Test
  public void eqRightConstant() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP2, TEMP1, TokenType.EQEQ, ConstantOperand.ONE, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }
}
