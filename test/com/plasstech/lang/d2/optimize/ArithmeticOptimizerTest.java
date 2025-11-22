package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static com.plasstech.lang.d2.codegen.ConstantOperand.EMPTY_STRING;
import static com.plasstech.lang.d2.codegen.ConstantOperand.FALSE;
import static com.plasstech.lang.d2.codegen.ConstantOperand.ONE;
import static com.plasstech.lang.d2.codegen.ConstantOperand.TRUE;
import static com.plasstech.lang.d2.optimize.testing.OpcodeSubject.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThatInterpreting;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.Range;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.testing.IntegralTypeProvider;
import com.plasstech.lang.d2.type.testing.NumericTypeProvider;

@RunWith(TestParameterInjector.class)
public class ArithmeticOptimizerTest {
  private final Optimizer optimizer = new ArithmeticOptimizer(0);

  private static final TempLocation INT1 = LocationUtils.newTempLocation("int1", VarType.INT);
  private static final TempLocation INT2 = LocationUtils.newTempLocation("int2", VarType.INT);
  private static final TempLocation STR1 = LocationUtils.newTempLocation("str1", VarType.STRING);
  private static final TempLocation STR2 = LocationUtils.newTempLocation("str2", VarType.STRING);
  private static final TempLocation DBL1 = LocationUtils.newTempLocation("dbl1", VarType.DOUBLE);

  private static final ConstantOperand<String> CONSTANT_A = ConstantOperand.of("a");
  private static final ConstantOperand<String> CONSTANT_B = ConstantOperand.of("b");
  private static final Operand CONSTANT_RANGE =
      new ConstantOperand<Range>(new Range(1234, 2345), VarType.RANGE);

  private static final Operand NULL_OPERAND = new ConstantOperand<Void>(null, VarType.NULL);

  @Test
  public void varPlusVarBecomesShift() {
    ImmutableList<Op> program = ImmutableList.of(new BinOp(INT1, INT2, TokenType.PLUS, INT2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(INT1, INT2, TokenType.SHIFT_LEFT, ONE);
  }

  @Test
  public void constPlusConst(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(new BinOp(DBL1, one, TokenType.PLUS, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(2, varType));
  }

  @Test
  public void bitNotConst(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(4, varType);
    ImmutableList<Op> program = ImmutableList.of(new UnaryOp(DBL1, TokenType.BIT_NOT, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(~4, varType));
  }

  @Test
  public void doubleConstPlusConst() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                DBL1, ConstantOperand.of(1.23), TokenType.PLUS, ConstantOperand.of(234.56), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(235.79));
  }

  @Test
  public void constMinusConst(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(new BinOp(DBL1, one, TokenType.MINUS, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(0, varType));
  }

  @Test
  public void constMultConst(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                DBL1,
                ConstantOperand.fromValue(2, varType),
                TokenType.MULT,
                ConstantOperand.fromValue(3, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(6, varType));
  }

  @Test
  public void multByPowerOf2() {
    // x * 8 == x << 3
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, INT2, TokenType.MULT, ConstantOperand.of(8), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(INT1, INT2, TokenType.SHIFT_LEFT, ConstantOperand.of(3));
  }

  @Test
  public void multByPowerOf2Long() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1,
                LocationUtils.newTempLocation("long", VarType.LONG),
                TokenType.MULT,
                ConstantOperand.of(8L),
                null));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void divByPowerOf2() {
    // x / 128 == x >> 7
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, INT2, TokenType.DIV, ConstantOperand.of(128), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(INT1, INT2, TokenType.SHIFT_RIGHT, ConstantOperand.of(7));
  }

  @Test
  public void divByPowerOf2Long() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1,
                LocationUtils.newTempLocation("long", VarType.LONG),
                TokenType.DIV,
                ConstantOperand.of(8L),
                null));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void constModConst(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                DBL1,
                ConstantOperand.fromValue(12, varType),
                TokenType.MOD,
                ConstantOperand.fromValue(5, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(12 % 5, varType));
  }

  @Test
  public void constDivConst(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                DBL1,
                ConstantOperand.fromValue(12, varType),
                TokenType.DIV,
                ConstantOperand.fromValue(3, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(4, varType));
  }

  @Test
  public void constDivConstLessThan1(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                DBL1,
                ConstantOperand.fromValue(1, varType),
                TokenType.DIV,
                ConstantOperand.fromValue(6, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(0, varType));
  }

  @Test
  public void constDivConstNegativeLessThan1(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                DBL1,
                ConstantOperand.fromValue(-1, varType),
                TokenType.DIV,
                ConstantOperand.fromValue(-6, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(0, varType));
  }

  @Test
  public void divItself(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand operand = LocationUtils.newTempLocation("operand", varType);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(dest, operand, TokenType.DIV, operand, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(1, varType));
  }

  @Test
  public void zeroDivVar(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand operand = LocationUtils.newTempLocation("operand", varType);
    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(dest, zero, TokenType.DIV, operand, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(zero);
  }

  @Test
  public void modOrSubItself(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType,
      @TestParameter({"MOD", "MINUS"}) TokenType operator) {

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand operand = LocationUtils.newTempLocation("operand", varType);

    ImmutableList<Op> program = ImmutableList.of(new BinOp(dest, operand, operator, operand, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);
    assertThat(optimized.get(0)).isTransferredFrom(zero);
  }

  @Test
  public void varPlusVarStrings() {
    ImmutableList<Op> program = ImmutableList.of(new BinOp(INT1, STR1, TokenType.PLUS, STR1, null));
    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void varPlusEmptyStringRight() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, STR1, TokenType.PLUS, EMPTY_STRING, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isTransferredFrom(STR1);
  }

  @Test
  public void varPlusEmptyStringLeft() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, EMPTY_STRING, TokenType.PLUS, STR1, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isTransferredFrom(STR1);
  }

  @Test
  public void constStringPlusConstString(
      @TestParameter({"a", ""}) String leftValue, @TestParameter({"", "b"}) String rightValue) {

    Operand left = ConstantOperand.of(leftValue);
    Operand right = ConstantOperand.of(rightValue);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(STR1, left, TokenType.PLUS, right, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(leftValue + rightValue));
  }

  @Test
  public void compareToItself() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(INT1, INT2, TokenType.EQEQ, INT2, null),
            new BinOp(INT1, INT2, TokenType.NEQ, INT2, null),
            new BinOp(INT1, INT2, TokenType.LEQ, INT2, null),
            new BinOp(INT1, INT2, TokenType.GEQ, INT2, null),
            new BinOp(INT1, INT2, TokenType.LT, INT2, null),
            new BinOp(INT1, INT2, TokenType.GT, INT2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(6);
    assertThat(optimized.get(0)).isTransferredFrom(TRUE);
    assertThat(optimized.get(1)).isTransferredFrom(FALSE);
    assertThat(optimized.get(2)).isTransferredFrom(TRUE);
    assertThat(optimized.get(3)).isTransferredFrom(TRUE);
    assertThat(optimized.get(4)).isTransferredFrom(FALSE);
    assertThat(optimized.get(5)).isTransferredFrom(FALSE);
  }

  @Test
  public void compareBooleansTrueTrue() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(INT1, TRUE, TokenType.EQEQ, TRUE, null),
            new BinOp(INT1, TRUE, TokenType.NEQ, TRUE, null),
            new BinOp(INT1, TRUE, TokenType.LEQ, TRUE, null),
            new BinOp(INT1, TRUE, TokenType.GEQ, TRUE, null),
            new BinOp(INT1, TRUE, TokenType.LT, TRUE, null),
            new BinOp(INT1, TRUE, TokenType.GT, TRUE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(6);
    assertThat(optimized.get(0)).isTransferredFrom(TRUE);
    assertThat(optimized.get(1)).isTransferredFrom(FALSE);
    assertThat(optimized.get(2)).isTransferredFrom(TRUE);
    assertThat(optimized.get(3)).isTransferredFrom(TRUE);
    assertThat(optimized.get(4)).isTransferredFrom(FALSE);
    assertThat(optimized.get(5)).isTransferredFrom(FALSE);
  }

  @Test
  public void compareBooleansFalseTrue() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(INT1, FALSE, TokenType.EQEQ, TRUE, null),
            new BinOp(INT1, FALSE, TokenType.NEQ, TRUE, null),
            new BinOp(INT1, FALSE, TokenType.LEQ, TRUE, null),
            new BinOp(INT1, FALSE, TokenType.GEQ, TRUE, null),
            new BinOp(INT1, FALSE, TokenType.LT, TRUE, null),
            new BinOp(INT1, FALSE, TokenType.GT, TRUE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(6);
    assertThat(optimized.get(0)).isTransferredFrom(FALSE);
    assertThat(optimized.get(1)).isTransferredFrom(TRUE);
    assertThat(optimized.get(2)).isTransferredFrom(TRUE);
    assertThat(optimized.get(3)).isTransferredFrom(FALSE);
    assertThat(optimized.get(4)).isTransferredFrom(TRUE);
    assertThat(optimized.get(5)).isTransferredFrom(FALSE);
  }

  @Test
  public void eqeqConstant(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(INT1, one, TokenType.EQEQ, one, null),
            new BinOp(INT1, zero, TokenType.EQEQ, one, null),
            new BinOp(INT1, one, TokenType.NEQ, one, null),
            new BinOp(INT1, zero, TokenType.NEQ, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(4);
    assertThat(optimized.get(0)).isTransferredFrom(TRUE);
    assertThat(optimized.get(1)).isTransferredFrom(FALSE);
    assertThat(optimized.get(2)).isTransferredFrom(FALSE);
    assertThat(optimized.get(3)).isTransferredFrom(TRUE);
  }

  @Test
  public void compareConstantNumbers(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    Location dest = LocationUtils.newMemoryAddress("dest", VarType.BOOL);
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(dest, one, TokenType.GT, zero, null),
            new BinOp(dest, zero, TokenType.GT, one, null),
            new BinOp(dest, one, TokenType.LT, zero, null),
            new BinOp(dest, zero, TokenType.LT, one, null),
            new BinOp(dest, one, TokenType.LEQ, zero, null),
            new BinOp(dest, zero, TokenType.LEQ, one, null),
            new BinOp(dest, one, TokenType.GEQ, zero, null),
            new BinOp(dest, zero, TokenType.GEQ, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(8);
    assertThat(optimized.get(0)).isTransferredFrom(TRUE);
    assertThat(optimized.get(1)).isTransferredFrom(FALSE);
    assertThat(optimized.get(2)).isTransferredFrom(FALSE);
    assertThat(optimized.get(3)).isTransferredFrom(TRUE);
    assertThat(optimized.get(4)).isTransferredFrom(FALSE);
    assertThat(optimized.get(5)).isTransferredFrom(TRUE);
    assertThat(optimized.get(6)).isTransferredFrom(TRUE);
    assertThat(optimized.get(7)).isTransferredFrom(FALSE);
  }

  @Test
  public void compareConstStringsLeqGeq() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(INT1, CONSTANT_B, TokenType.LEQ, CONSTANT_A, null),
            new BinOp(INT1, CONSTANT_A, TokenType.LEQ, CONSTANT_B, null),
            new BinOp(INT1, CONSTANT_B, TokenType.GEQ, CONSTANT_A, null),
            new BinOp(INT1, CONSTANT_A, TokenType.GEQ, CONSTANT_B, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(4);
    assertThat(optimized.get(0)).isTransferredFrom(FALSE);
    assertThat(optimized.get(1)).isTransferredFrom(TRUE);
    assertThat(optimized.get(2)).isTransferredFrom(TRUE);
    assertThat(optimized.get(3)).isTransferredFrom(FALSE);
  }

  @Test
  public void compareConstStringsGtLt() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(INT1, CONSTANT_B, TokenType.GT, CONSTANT_A, null),
            new BinOp(INT1, CONSTANT_A, TokenType.GT, CONSTANT_B, null),
            new BinOp(INT1, CONSTANT_B, TokenType.LT, CONSTANT_A, null),
            new BinOp(INT1, CONSTANT_A, TokenType.LT, CONSTANT_B, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(4);
    assertThat(optimized.get(0)).isTransferredFrom(TRUE);
    assertThat(optimized.get(1)).isTransferredFrom(FALSE);
    assertThat(optimized.get(2)).isTransferredFrom(FALSE);
    assertThat(optimized.get(3)).isTransferredFrom(TRUE);
  }

  @Test
  public void constBitAndConst(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1,
                ConstantOperand.fromValue(111, varType),
                TokenType.BIT_AND,
                ConstantOperand.fromValue(4, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(4, varType));
  }

  @Test
  public void constBoolAndConst(@TestParameter boolean left, @TestParameter boolean right) {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1, ConstantOperand.of(left), TokenType.AND, ConstantOperand.of(right), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(left && right));
  }

  @Test
  public void constBoolOrConst(@TestParameter boolean left, @TestParameter boolean right) {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1, ConstantOperand.of(left), TokenType.OR, ConstantOperand.of(right), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(left || right));
  }

  @Test
  public void constBoolXorConst(@TestParameter boolean left, @TestParameter boolean right) {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1, ConstantOperand.of(left), TokenType.XOR, ConstantOperand.of(right), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(left ^ right));
  }

  @Test
  public void boolAndTrue() {
    Location left = LocationUtils.newMemoryAddress("left", VarType.BOOL);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, left, TokenType.AND, ConstantOperand.TRUE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(left);
  }

  @Test
  public void boolWithItself(@TestParameter({"AND", "OR"}) TokenType tokenType) {
    Location left = LocationUtils.newMemoryAddress("left", VarType.BOOL);
    ImmutableList<Op> program = ImmutableList.of(new BinOp(INT1, left, tokenType, left, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(left);
  }

  @Test
  public void boolAndFalse() {
    Location left = LocationUtils.newMemoryAddress("left", VarType.BOOL);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, left, TokenType.AND, ConstantOperand.FALSE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.FALSE);
  }

  @Test
  public void boolOrTrue() {
    Location left = LocationUtils.newMemoryAddress("left", VarType.BOOL);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, left, TokenType.OR, ConstantOperand.TRUE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.TRUE);
  }

  @Test
  public void boolOrFalse() {
    Location left = LocationUtils.newMemoryAddress("left", VarType.BOOL);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, left, TokenType.OR, ConstantOperand.FALSE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(left);
  }

  @Test
  public void constBitOrConst(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1,
                ConstantOperand.fromValue(111, varType),
                TokenType.BIT_OR,
                ConstantOperand.fromValue(4, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(111, varType));
  }

  @Test
  public void constBitXorConst(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1,
                ConstantOperand.fromValue(111, varType),
                TokenType.BIT_XOR,
                ConstantOperand.fromValue(4, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(4 ^ 111, varType));
  }

  @Test
  public void constShiftLeftConst(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1,
                ConstantOperand.fromValue(111, varType),
                TokenType.SHIFT_LEFT,
                ConstantOperand.fromValue(4, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(111 << 4, varType));
  }

  @Test
  public void constShiftRightConst(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1,
                ConstantOperand.fromValue(120, varType),
                TokenType.SHIFT_RIGHT,
                ConstantOperand.fromValue(3, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(120 >> 3, varType));
  }

  @Test
  public void emptyAsc() {
    assertThatInterpreting("c=asc('')")
        .withOptimizer(optimizer)
        .hasCompileTimeError("Cannot take ASC of empty STRING");
  }

  @Test
  public void constAsc() {
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(INT1, TokenType.ASC, ConstantOperand.of("ABC"), null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(65));
  }

  @Test
  public void modItself(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    Location left = LocationUtils.newParamLocation("operand", varType, 0, 0);
    ImmutableList<Op> program = ImmutableList.of(new BinOp(left, left, TokenType.MOD, left, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(0, varType));
  }

  @Test
  public void mod1(@TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT2,
                ConstantOperand.fromValue(14, varType),
                TokenType.MOD,
                ConstantOperand.fromValue(1, varType),
                null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(0, varType));
  }

  @Test
  public void multDivOne(
      @TestParameter({"MULT", "DIV"}) TokenType operand,
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand left = LocationUtils.newTempLocation("operand", varType);
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);

    ImmutableList<Op> program = ImmutableList.of(new BinOp(dest, left, operand, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(left);
  }

  @Test
  public void multDivNegOne(
      @TestParameter({"MULT", "DIV"}) TokenType operand,
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand left = LocationUtils.newTempLocation("operand", varType);
    ConstantOperand<? extends Number> negOne = ConstantOperand.fromValue(-1, varType);

    ImmutableList<Op> program = ImmutableList.of(new BinOp(dest, left, operand, negOne, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isUnaryOp(dest, TokenType.MINUS, left);
  }

  @Test
  public void multDivNegTwo(
      @TestParameter({"MULT", "DIV"}) TokenType operand,
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand left = LocationUtils.newTempLocation("operand", varType);
    ConstantOperand<? extends Number> negOne = ConstantOperand.fromValue(-2, varType);

    ImmutableList<Op> program = ImmutableList.of(new BinOp(dest, left, operand, negOne, null));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void opZeroUnchanged(
      @TestParameter({"PLUS", "MINUS", "SHIFT_LEFT", "SHIFT_RIGHT", "BIT_XOR", "BIT_OR"})
          TokenType operator,
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    assume()
        .that(
            (operator == TokenType.SHIFT_LEFT
                    || operator == TokenType.BIT_XOR
                    || operator == TokenType.SHIFT_RIGHT
                    || operator == TokenType.BIT_OR)
                && !varType.isIntegral())
        .isFalse();

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand left = LocationUtils.newTempLocation("operand", varType);
    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);

    ImmutableList<Op> program = ImmutableList.of(new BinOp(dest, left, operator, zero, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(left);
  }

  @Test
  public void unaryPlusMinus(
      @TestParameter({"PLUS", "MINUS"}) TokenType operator,
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program = ImmutableList.of(new UnaryOp(INT1, operator, one, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    if (operator == TokenType.PLUS) {
      // Byte me.
      assertThat(optimized.get(0)).isTransferredFrom(one);
    } else {
      assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(-1, varType));
    }
  }

  @Test
  public void varOpZeroIsZero(
      @TestParameter({"MULT", "BIT_AND"}) TokenType operator,
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    assume().that(operator == TokenType.BIT_AND && !varType.isIntegral()).isFalse();

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand left = LocationUtils.newTempLocation("operand", varType);
    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);

    ImmutableList<Op> program = ImmutableList.of(new BinOp(dest, left, operator, zero, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(zero);
  }

  @Test
  public void zeroSubOperandToUnary(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand right = LocationUtils.newTempLocation("operand", varType);
    ConstantOperand<? extends Number> zero = ConstantOperand.zeroOf(varType);

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(dest, zero, TokenType.MINUS, right, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isUnaryOp(dest, TokenType.MINUS, right);
  }

  @Test
  public void zeroDoubleSubOperandToUnary() {

    Location dest = LocationUtils.newTempLocation("dest", VarType.DOUBLE);
    Operand right = LocationUtils.newTempLocation("operand", VarType.DOUBLE);
    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, VarType.DOUBLE);

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(dest, zero, TokenType.MINUS, right, null));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void constantArrayLength() {
    ArrayType arrayType = new ArrayType(VarType.INT, 1).setKnownLength(3);
    Operand constArray = LocationUtils.newMemoryAddress("array", arrayType);
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(INT1, TokenType.LENGTH, constArray, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(3));
  }

  @Test
  public void variableArrayLength() {
    ArrayType arrayType = new ArrayType(VarType.INT, 1);
    Operand constArray = LocationUtils.newMemoryAddress("array", arrayType);
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(INT1, TokenType.LENGTH, constArray, null));

    optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void rangeLength() {
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(INT1, TokenType.LENGTH, CONSTANT_RANGE, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(2));
  }

  @Test
  public void rangeConstantIndex() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(INT1, CONSTANT_RANGE, TokenType.LBRACKET, ConstantOperand.of(0), null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(1234));
  }

  @Test
  public void constantStringConstantRange() {
    Location stringResult = LocationUtils.newStackLocation("string", VarType.STRING, 8);
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                stringResult,
                ConstantOperand.of("123456"),
                TokenType.LBRACKET,
                new ConstantOperand<Range>(new Range(2, 4), VarType.RANGE),
                null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of("34"));
  }

  @Test
  public void constantStringEmptyRange() {
    Location stringResult = LocationUtils.newStackLocation("string", VarType.STRING, 8);
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                stringResult,
                ConstantOperand.of("123456"),
                TokenType.LBRACKET,
                new ConstantOperand<Range>(new Range(2, 2), VarType.RANGE),
                null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(""));
  }

  @Test
  public void constantStringLength() {
    Location stringResult = LocationUtils.newStackLocation("string", VarType.STRING, 8);
    ImmutableList<Op> program =
        ImmutableList.of(
            new UnaryOp(stringResult, TokenType.LENGTH, ConstantOperand.of("abc"), null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(3));
  }

  @Test
  public void constantStringIndex() {
    Location stringResult = LocationUtils.newStackLocation("string", VarType.STRING, 8);
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                stringResult,
                ConstantOperand.of("abc"),
                TokenType.LBRACKET,
                ConstantOperand.of(1),
                null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of("b"));
  }

  @Test
  public void constantColon() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                INT1, ConstantOperand.of(123), TokenType.COLON, ConstantOperand.of(234), null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).hasSize(1);
    Range range = new Range(123, 234);
    ConstantOperand<Range> constRange = new ConstantOperand<Range>(range, VarType.RANGE);
    assertThat(optimized.get(0)).isTransferredFrom(constRange);
  }

  @Test
  public void constChr() {
    Location stringResult = LocationUtils.newStackLocation("string", VarType.STRING, 8);
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(stringResult, TokenType.CHR, ConstantOperand.of(42), null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of("*"));
  }

  @Test
  public void nonConstChr() {
    Location stringResult = LocationUtils.newStackLocation("string", VarType.STRING, 8);
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(stringResult, TokenType.CHR, INT1, null));
    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  // not true is false
  @Test
  public void unaryNotConstant(@TestParameter boolean value) {
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(INT1, TokenType.NOT, ConstantOperand.of(value), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(!value));
  }

  // 10-anything is unchnaged
  @Test
  public void notZeroSubOperandUnchanged(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand right = LocationUtils.newTempLocation("operand", varType);

    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(dest, ConstantOperand.fromValue(10, varType), TokenType.MINUS, right, null));

    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void plusPositiveIsUnchanged() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(INT1, INT2, TokenType.PLUS, ConstantOperand.of(2), null));
    optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void multiplyNegativeIsUnchanged() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(INT1, INT2, TokenType.MULT, ConstantOperand.of(-2), null));
    optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void addingStringsAreNotChanged() {
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(STR1, STR2, TokenType.PLUS, ConstantOperand.of("hi"), null));
    optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void plusNegativeBecomesSubtraction(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    Location loc = LocationUtils.newTempLocation("loc", varType);
    ImmutableList<Op> input =
        ImmutableList.of(
            new BinOp(loc, loc, TokenType.PLUS, ConstantOperand.fromValue(-2, varType), null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0))
        .isBinOp(loc, loc, TokenType.MINUS, ConstantOperand.fromValue(2, varType));
  }

  @Test
  public void minusNegativeBecomesAddition(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    Location loc = LocationUtils.newTempLocation("loc", varType);
    ImmutableList<Op> input =
        ImmutableList.of(
            new BinOp(loc, loc, TokenType.MINUS, ConstantOperand.fromValue(-2, varType), null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0))
        .isBinOp(loc, loc, TokenType.PLUS, ConstantOperand.fromValue(2, varType));
  }

  @Test
  public void coalesceNullLhs() {
    // null??anything -> anything
    Location loc = LocationUtils.newTempLocation("loc", VarType.STRING);
    ConstantOperand<String> rhs = ConstantOperand.of("hi");
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(loc, ConstantOperand.NULL, TokenType.NULL_COALESCE, rhs, null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(rhs);
  }

  @Test
  public void coalesceNullRhs() {
    // anything??null -> anything
    Location loc = LocationUtils.newTempLocation("loc", VarType.STRING);
    ConstantOperand<String> lhs = ConstantOperand.of("hi");
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(loc, lhs, TokenType.NULL_COALESCE, ConstantOperand.NULL, null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(lhs);
  }

  @Test
  public void coalesceNulls() {
    // null??null-> null
    Location loc = LocationUtils.newTempLocation("loc", VarType.STRING);
    ImmutableList<Op> input =
        ImmutableList.of(
            new BinOp(
                loc, ConstantOperand.NULL, TokenType.NULL_COALESCE, ConstantOperand.NULL, null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.NULL);
  }

  @Test
  public void coalesceSame() {
    // anything??anything-> anything
    Location loc = LocationUtils.newTempLocation("loc", VarType.STRING);
    ConstantOperand<String> lhs = ConstantOperand.of("hi");
    ImmutableList<Op> input =
        ImmutableList.of(new BinOp(loc, lhs, TokenType.NULL_COALESCE, lhs, null));
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(lhs);
  }

  @Test
  public void addNullToEmptyString() {
    // 'thing'+null-> "thingnull"
    Location dest = LocationUtils.newTempLocation("loc", VarType.STRING);
    BinOp op = new BinOp(dest, ConstantOperand.of("thing"), TokenType.PLUS, NULL_OPERAND, null);
    ImmutableList<Op> input = ImmutableList.of(op);
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0))
        .isBinOp(dest, op.left(), op.operator(), ConstantOperand.of("null"));
  }

  @Test
  public void addEmptyStringToNull() {
    // null+'thing'-> "nullthing"
    Location dest = LocationUtils.newTempLocation("loc", VarType.STRING);
    BinOp op = new BinOp(dest, NULL_OPERAND, TokenType.PLUS, ConstantOperand.of("thing"), null);
    ImmutableList<Op> input = ImmutableList.of(op);
    ImmutableList<Op> optimized = optimizer.optimize(input, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0))
        .isBinOp(dest, ConstantOperand.of("null"), op.operator(), op.right());
  }
}
