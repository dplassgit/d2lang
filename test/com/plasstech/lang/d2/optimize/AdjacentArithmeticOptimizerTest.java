package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static com.plasstech.lang.d2.optimize.OpcodeSubject.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.testing.NumericTypeProvider;

@RunWith(TestParameterInjector.class)
public class AdjacentArithmeticOptimizerTest {
  private final Optimizer OPTIMIZERS =
      new ILOptimizer(
          ImmutableList.of(new AdjacentArithmeticOptimizer(2), new NopOptimizer()))
          .setDebugLevel(2);

  @TestParameter(valuesProvider = NumericTypeProvider.class)
  VarType varType;

  private static final TempLocation TEMP1 = LocationUtils.newTempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = LocationUtils.newTempLocation("temp2", VarType.INT);
  private static final TempLocation TEMP3 = LocationUtils.newTempLocation("temp3", VarType.INT);
  private static final Location VAR1 = LocationUtils.newMemoryAddress("a", VarType.INT);
  private static final Location VAR2 = LocationUtils.newMemoryAddress("b", VarType.INT);

  @Test
  public void plusPlus() {
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1+2
        new BinOp(TEMP2, TEMP1, TokenType.PLUS, one, null),
        new BinOp(TEMP3, TEMP2, TokenType.PLUS, one, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    ConstantOperand<? extends Number> two = ConstantOperand.fromValue(2, varType);
    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.PLUS, two);
  }

  @Test
  public void plusInc() {
    assume().that(varType.isIntegral()).isTrue();

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp2=temp2+2, which yes, isn't possible in the 'real world'
        new BinOp(TEMP2, TEMP2, TokenType.PLUS, one, null),
        new Inc(TEMP2, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    ConstantOperand<? extends Number> two = ConstantOperand.fromValue(2, varType);
    assertThat(optimized.get(0)).isBinOp(TEMP2, TEMP2, TokenType.PLUS, two);
  }

  @Test
  public void plusDec() {
    assume().that(varType.isIntegral()).isTrue();

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp2=temp2+0, which yes, isn't possible in the 'real world'
        new BinOp(TEMP2, TEMP2, TokenType.PLUS, one, null),
        new Dec(TEMP2, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);
    assertThat(optimized.get(0)).isBinOp(TEMP2, TEMP2, TokenType.PLUS, zero);
  }

  @Test
  public void incPlusDifferent_noChange() {
    assume().that(varType.isIntegral()).isTrue();

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(
        new Inc(VAR1, null),
        new BinOp(TEMP2, VAR1, TokenType.PLUS, one, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void plusPlusDifferent_noChange() {
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // var=var+1
        // temp2=var+1
        // should have no change.
        new BinOp(VAR1, VAR1, TokenType.PLUS, one, null),
        new BinOp(TEMP2, VAR1, TokenType.PLUS, one, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void plusPlusSame() {
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // var=var+1
        // var=var+1
        new BinOp(VAR1, VAR1, TokenType.PLUS, one, null),
        new BinOp(VAR1, VAR1, TokenType.PLUS, one, null));

    OPTIMIZERS.optimize(program, null);
    // should become var = var + 2 - this is handled by AdjacentIncDecOptimizer
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void decIncDifferent() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program = ImmutableList.of(new Dec(VAR1, null), new Inc(VAR2, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void incDecDifferent() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program = ImmutableList.of(new Inc(VAR1, null), new Dec(VAR2, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void orOr() {
    assume().that(varType.isIntegral()).isTrue();

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ConstantOperand<? extends Number> three = ConstantOperand.fromValue(3, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1|3
        new BinOp(TEMP2, TEMP1, TokenType.BIT_OR, one, null),
        new BinOp(TEMP3, TEMP2, TokenType.BIT_OR, three, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.BIT_OR, three);
  }

  @Test
  public void andAnd() {
    assume().that(varType.isIntegral()).isTrue();

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ConstantOperand<? extends Number> three = ConstantOperand.fromValue(3, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1&3
        new BinOp(TEMP2, TEMP1, TokenType.BIT_AND, one, null),
        new BinOp(TEMP3, TEMP2, TokenType.BIT_AND, three, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.BIT_AND, one);
  }

  @Test
  public void xorXor() {
    assume().that(varType.isIntegral()).isTrue();

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ConstantOperand<? extends Number> four = ConstantOperand.fromValue(4, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1^4
        new BinOp(TEMP2, TEMP1, TokenType.BIT_XOR, one, null),
        new BinOp(TEMP3, TEMP2, TokenType.BIT_XOR, four, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.BIT_XOR,
        ConstantOperand.fromValue(5, varType));
  }

  @Test
  public void multMult() {
    ConstantOperand<? extends Number> two = ConstantOperand.fromValue(2, varType);
    ConstantOperand<? extends Number> three = ConstantOperand.fromValue(3, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1*6
        new BinOp(TEMP2, TEMP1, TokenType.MULT, two, null),
        new BinOp(TEMP3, TEMP2, TokenType.MULT, three, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    ConstantOperand<? extends Number> six = ConstantOperand.fromValue(6, varType);
    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.MULT, six);
  }

  @Test
  public void plusMult() {
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(TEMP2, TEMP1, TokenType.PLUS, one, null),
        new BinOp(TEMP3, TEMP2, TokenType.MULT, one, null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void plusMinus() {
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1+0
        new BinOp(TEMP2, TEMP1, TokenType.PLUS, one, null),
        new BinOp(TEMP3, TEMP2, TokenType.MINUS, one, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);
    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.PLUS, zero);
  }

  @Test
  public void minusPlus() {
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1+0
        new BinOp(TEMP2, TEMP1, TokenType.MINUS, one, null),
        new BinOp(TEMP3, TEMP2, TokenType.PLUS, one, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);
    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.MINUS, zero);
  }

  @Test
  public void minusPlus2() {
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ConstantOperand<? extends Number> two = ConstantOperand.fromValue(2, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should become temp3=temp1=2+1 = -1
        new BinOp(TEMP2, TEMP1, TokenType.MINUS, two, null),
        new BinOp(TEMP3, TEMP2, TokenType.PLUS, one, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.MINUS, one);
  }

  @Test
  public void minusMinus() {
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 - 2
        new BinOp(TEMP2, TEMP1, TokenType.MINUS, one, null),
        new BinOp(TEMP3, TEMP2, TokenType.MINUS, one, null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    ConstantOperand<? extends Number> two = ConstantOperand.fromValue(2, varType);
    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.MINUS, two);
  }

  @Test
  public void divDiv() {
    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 / 10
        new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.fromValue(5, varType), null),
        new BinOp(TEMP3, TEMP2, TokenType.DIV, ConstantOperand.fromValue(2, varType), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.DIV,
        ConstantOperand.fromValue(10, varType));
  }

  @Test
  public void divMult() {
    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 / (20/10) = temp1 / 2
        new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.fromValue(20, varType), null),
        new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.fromValue(10, varType), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.DIV,
        ConstantOperand.fromValue(2, varType));
  }

  @Test
  public void divMultTooSmall() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 / (2/10) = temp1 / 0 behnt.
        new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.fromValue(2, varType), null),
        new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.fromValue(10, varType), null));

    OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isFalse();
  }

  @Test
  public void divMultTooSmall_double() {
    assume().that(varType).isEqualTo(VarType.DOUBLE);

    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 / 10
        new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.fromValue(2, varType), null),
        new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.fromValue(10, varType), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.DIV,
        ConstantOperand.of(0.2));
  }

  @Test
  public void multDiv() {
    ImmutableList<Op> program = ImmutableList.of(
        // should be temp3 = temp1 * (20/5) = temp1 * 4
        new BinOp(TEMP2, TEMP1, TokenType.MULT, ConstantOperand.fromValue(20, varType), null),
        new BinOp(TEMP3, TEMP2, TokenType.DIV, ConstantOperand.fromValue(5, varType), null));

    ImmutableList<Op> optimized = OPTIMIZERS.optimize(program, null);
    assertThat(OPTIMIZERS.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.MULT,
        ConstantOperand.fromValue(4, varType));
  }
}
