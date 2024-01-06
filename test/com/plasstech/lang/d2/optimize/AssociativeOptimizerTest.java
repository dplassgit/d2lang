package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.codegen.ConstantOperand.ONE;
import static com.plasstech.lang.d2.optimize.OpcodeSubject.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

@RunWith(TestParameterInjector.class)
public class AssociativeOptimizerTest {
  private Optimizer optimizer = new AssociativeOptimizer(2);

  private static final Location TEMP1 = LocationUtils.newTempLocation("temp1", VarType.INT);
  private static final Location TEMP2 = LocationUtils.newTempLocation("temp2", VarType.INT);
  private static final Location STRING_TEMP =
      LocationUtils.newTempLocation("stringtemp", VarType.STRING);

  @Test
  public void varOpConst_doesNotSwap(
      @TestParameter(
        {"PLUS", "MULT", "BIT_AND", "BIT_OR", "BIT_XOR", "EQEQ", "NEQ"}
      ) TokenType operator) {

    ImmutableList<Op> program = ImmutableList.of(new BinOp(TEMP1, TEMP1, operator, ONE, null));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void constOpVar_swapsInts(
      @TestParameter(
        {"PLUS", "MULT", "BIT_AND", "BIT_OR", "BIT_XOR", "EQEQ", "NEQ"}
      ) TokenType operator) {

    ImmutableList<Op> program = ImmutableList.of(new BinOp(TEMP1, ONE, operator, TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized.get(0)).isBinOp(TEMP1, TEMP1, operator, ONE);
  }

  @Test
  public void constOpVar_swapsBools(@TestParameter({"AND", "OR", "XOR"}) TokenType op) {

    Location booltemp = LocationUtils.newTempLocation("booltemp", VarType.BOOL);
    Location booltemp2 = LocationUtils.newTempLocation("booltemp2", VarType.BOOL);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(booltemp, ConstantOperand.TRUE, op, booltemp2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized.get(0)).isBinOp(booltemp, booltemp2, op, ConstantOperand.TRUE);
  }

  @Test
  public void constOpVar_doesNotSwapStrings() {
    ImmutableList<Op> program =
        ImmutableList
            .of(new BinOp(STRING_TEMP, ConstantOperand.of("hi"), TokenType.PLUS, STRING_TEMP,
                null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void constOpVar_swapsStrings(@TestParameter({"EQEQ", "NEQ"}) TokenType operator) {

    Location booltemp = LocationUtils.newTempLocation("booltemp", VarType.BOOL);
    ConstantOperand<String> hi = ConstantOperand.of("hi");
    ImmutableList<Op> program =
        ImmutableList
            .of(new BinOp(booltemp, hi, operator, STRING_TEMP, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isBinOp(booltemp, STRING_TEMP, operator, hi);
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
    assertThat(optimized.get(0)).isBinOp(TEMP2, TEMP1, TokenType.LT, ConstantOperand.ONE);
  }

  @Test
  public void ltLeftConstant() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP2, ConstantOperand.ONE, TokenType.LT, TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isBinOp(TEMP2, TEMP1, TokenType.GT, ConstantOperand.ONE);
  }

  @Test
  public void lteLeftConstant() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP2, ConstantOperand.ONE, TokenType.LEQ, TEMP1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isBinOp(TEMP2, TEMP1, TokenType.GEQ, ConstantOperand.ONE);
  }
}
