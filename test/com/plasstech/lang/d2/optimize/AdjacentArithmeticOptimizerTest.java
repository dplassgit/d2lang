package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static com.plasstech.lang.d2.optimize.testing.OpcodeSubject.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.optimize.testing.OptimizerWithNop;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.testing.NumericTypeProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class AdjacentArithmeticOptimizerTest {
  private final Optimizer optimizer = new OptimizerWithNop(new AdjacentArithmeticOptimizer(2));

  private static final TempLocation TEMP1 = LocationUtils.newTempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = LocationUtils.newTempLocation("temp2", VarType.INT);
  private static final TempLocation TEMP3 = LocationUtils.newTempLocation("temp3", VarType.INT);
  private static final Location VAR1 = LocationUtils.newMemoryAddress("a", VarType.INT);
  private static final Location VAR2 = LocationUtils.newMemoryAddress("b", VarType.INT);

  @TestParameter(valuesProvider = NumericTypeProvider.class)
  VarType varType;

  private Location typedVar1;
  private Operand zero;
  private Operand one;
  private Operand two;
  private Operand three;

  @Before
  public void setUp() {
    typedVar1 = LocationUtils.newStackLocation("typedvar1", varType, 0);
    zero = ConstantOperand.fromValue(0, varType);
    one = ConstantOperand.fromValue(1, varType);
    two = ConstantOperand.fromValue(2, varType);
    three = ConstantOperand.fromValue(3, varType);
  }

  @Test
  public void plusPlus() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp3=temp1+2
            new BinOp(TEMP2, TEMP1, TokenType.PLUS, one, null),
            new BinOp(TEMP3, TEMP2, TokenType.PLUS, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.PLUS, two);
  }

  @Test
  public void plusInc() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp2=temp2+2, which yes, isn't possible in the 'real world'
            new BinOp(TEMP2, TEMP2, TokenType.PLUS, one, null), new Inc(TEMP2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP2, TEMP2, TokenType.PLUS, two);
  }

  @Test
  public void inc() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program = ImmutableList.of(new Inc(TEMP2, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void plusDec() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp2=temp2+0, which yes, isn't possible in the 'real world'
            new BinOp(TEMP2, TEMP2, TokenType.PLUS, one, null), new Dec(TEMP2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP2, TEMP2, TokenType.PLUS, zero);
  }

  @Test
  public void incPlusDifferent_noChange() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(new Inc(VAR1, null), new BinOp(TEMP2, VAR1, TokenType.PLUS, one, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void plusPlusDifferent_noChange() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // var=var+1
            // temp2=var+1
            // should have no change.
            new BinOp(VAR1, VAR1, TokenType.PLUS, one, null),
            new BinOp(TEMP2, VAR1, TokenType.PLUS, one, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void plusPlusSame() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // var=var+1
            // var=var+1
            new BinOp(VAR1, VAR1, TokenType.PLUS, one, null),
            new BinOp(VAR1, VAR1, TokenType.PLUS, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isBinOp(VAR1, VAR1, TokenType.PLUS, two);
  }

  @Test
  public void decIncDifferent() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program = ImmutableList.of(new Dec(VAR1, null), new Inc(VAR2, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void incDecDifferent() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program = ImmutableList.of(new Inc(VAR1, null), new Dec(VAR2, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void orOr() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp3=temp1|3
            new BinOp(TEMP2, TEMP1, TokenType.BIT_OR, one, null),
            new BinOp(TEMP3, TEMP2, TokenType.BIT_OR, three, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.BIT_OR, three);
  }

  @Test
  public void andAnd() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp3=temp1&3
            new BinOp(TEMP2, TEMP1, TokenType.BIT_AND, one, null),
            new BinOp(TEMP3, TEMP2, TokenType.BIT_AND, three, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.BIT_AND, one);
  }

  @Test
  public void xorXor() {
    assume().that(varType.isIntegral()).isTrue();

    Operand four = ConstantOperand.fromValue(4, varType);
    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp3=temp1^4
            new BinOp(TEMP2, TEMP1, TokenType.BIT_XOR, one, null),
            new BinOp(TEMP3, TEMP2, TokenType.BIT_XOR, four, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0))
        .isBinOp(TEMP3, TEMP1, TokenType.BIT_XOR, ConstantOperand.fromValue(5, varType));
  }

  @Test
  public void multMult() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp3=temp1*6
            new BinOp(TEMP2, TEMP1, TokenType.MULT, two, null),
            new BinOp(TEMP3, TEMP2, TokenType.MULT, three, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    Operand six = ConstantOperand.fromValue(6, varType);
    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.MULT, six);
  }

  @Test
  public void plusMult() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP2, TEMP1, TokenType.PLUS, one, null),
            new BinOp(TEMP3, TEMP2, TokenType.MULT, one, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void plusMinus() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp3=temp1+0
            new BinOp(TEMP2, TEMP1, TokenType.PLUS, one, null),
            new BinOp(TEMP3, TEMP2, TokenType.MINUS, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.PLUS, zero);
  }

  @Test
  public void minusPlus() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp3=temp1+0
            new BinOp(TEMP2, TEMP1, TokenType.MINUS, one, null),
            new BinOp(TEMP3, TEMP2, TokenType.PLUS, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.MINUS, zero);
  }

  @Test
  public void minusPlus2() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should become temp3=temp1=2+1 = -1
            new BinOp(TEMP2, TEMP1, TokenType.MINUS, two, null),
            new BinOp(TEMP3, TEMP2, TokenType.PLUS, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.MINUS, one);
  }

  @Test
  public void minusMinus() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should be temp3 = temp1 - 2
            new BinOp(TEMP2, TEMP1, TokenType.MINUS, one, null),
            new BinOp(TEMP3, TEMP2, TokenType.MINUS, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.MINUS, two);
  }

  @Test
  public void divDiv() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should be temp3 = temp1 / 10
            new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.fromValue(5, varType), null),
            new BinOp(TEMP3, TEMP2, TokenType.DIV, two, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0))
        .isBinOp(TEMP3, TEMP1, TokenType.DIV, ConstantOperand.fromValue(10, varType));
  }

  @Test
  public void divMult() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should be temp3 = temp1 / (20/10) = temp1 / 2
            new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.fromValue(20, varType), null),
            new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.fromValue(10, varType), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.DIV, two);
  }

  @Test
  public void divMultTooSmall() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            // should be temp3 = temp1 / (2/10) = temp1 / 0 behnt.
            new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.fromValue(2, varType), null),
            new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.fromValue(10, varType), null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void divMultTooSmall_double() {
    assume().that(varType).isEqualTo(VarType.DOUBLE);

    ImmutableList<Op> program =
        ImmutableList.of(
            // should be temp3 = temp1 / 10
            new BinOp(TEMP2, TEMP1, TokenType.DIV, ConstantOperand.fromValue(2, varType), null),
            new BinOp(TEMP3, TEMP2, TokenType.MULT, ConstantOperand.fromValue(10, varType), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(TEMP3, TEMP1, TokenType.DIV, ConstantOperand.of(0.2));
  }

  @Test
  public void multDiv() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // should be temp3 = temp1 * (20/5) = temp1 * 4
            new BinOp(TEMP2, TEMP1, TokenType.MULT, ConstantOperand.fromValue(20, varType), null),
            new BinOp(TEMP3, TEMP2, TokenType.DIV, ConstantOperand.fromValue(5, varType), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0))
        .isBinOp(TEMP3, TEMP1, TokenType.MULT, ConstantOperand.fromValue(4, varType));
  }

  @Test
  public void twoIncs_differentVarsUnchanged() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program = ImmutableList.of(new Inc(VAR1, null), new Inc(VAR2, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void oneInc_unchanged() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program = ImmutableList.of(new Inc(VAR1, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void twoIncs() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(new Inc(typedVar1, null), new Inc(typedVar1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(typedVar1, typedVar1, TokenType.PLUS, two);
  }

  @Test
  public void incAdd() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            new Inc(typedVar1, null), new BinOp(typedVar1, typedVar1, TokenType.PLUS, two, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(typedVar1, typedVar1, TokenType.PLUS, three);
  }

  @Test
  public void addInc() {
    assume().that(varType.isIntegral()).isTrue();
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(typedVar1, typedVar1, TokenType.PLUS, two, null), new Inc(typedVar1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(typedVar1, typedVar1, TokenType.PLUS, three);
  }

  @Test
  public void addAdd() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(typedVar1, typedVar1, TokenType.PLUS, two, null),
            new BinOp(typedVar1, typedVar1, TokenType.PLUS, three, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0))
        .isBinOp(typedVar1, typedVar1, TokenType.PLUS, ConstantOperand.fromValue(5, varType));
  }

  @Test
  public void addDec() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(typedVar1, typedVar1, TokenType.PLUS, two, null), new Dec(typedVar1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isInc(typedVar1);
  }

  @Test
  public void addSub() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(typedVar1, typedVar1, TokenType.PLUS, two, null),
            new BinOp(
                typedVar1,
                typedVar1,
                TokenType.MINUS,
                ConstantOperand.fromValue(4, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized.get(0))
        .isBinOp(typedVar1, typedVar1, TokenType.PLUS, ConstantOperand.fromValue(-2, varType));
  }

  @Test
  public void incSub() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            new Inc(typedVar1, null), new BinOp(typedVar1, typedVar1, TokenType.MINUS, two, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized.get(0))
        .isBinOp(typedVar1, typedVar1, TokenType.PLUS, ConstantOperand.fromValue(-1, varType));
  }

  @Test
  public void twoDecs() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(new Dec(typedVar1, null), new Dec(typedVar1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(typedVar1, typedVar1, TokenType.MINUS, two);
  }

  @Test
  public void decMinus() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            new Dec(typedVar1, null),
            new BinOp(typedVar1, typedVar1, TokenType.MINUS, three, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0))
        .isBinOp(typedVar1, typedVar1, TokenType.MINUS, ConstantOperand.fromValue(4, varType));
  }

  @Test
  public void decPlus() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            new Dec(typedVar1, null), new BinOp(typedVar1, typedVar1, TokenType.PLUS, three, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0))
        .isBinOp(typedVar1, typedVar1, TokenType.MINUS, ConstantOperand.fromValue(-2, varType));
  }

  @Test
  public void incMinus() {
    assume().that(varType.isIntegral()).isTrue();

    ImmutableList<Op> program =
        ImmutableList.of(
            new Inc(typedVar1, null), new BinOp(typedVar1, typedVar1, TokenType.MINUS, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(typedVar1, typedVar1, TokenType.PLUS, zero);
  }
}
