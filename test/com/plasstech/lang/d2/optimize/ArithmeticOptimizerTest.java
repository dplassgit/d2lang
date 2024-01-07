package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.TruthJUnit.assume;
import static com.plasstech.lang.d2.codegen.ConstantOperand.EMPTY_STRING;
import static com.plasstech.lang.d2.codegen.ConstantOperand.FALSE;
import static com.plasstech.lang.d2.codegen.ConstantOperand.ONE;
import static com.plasstech.lang.d2.codegen.ConstantOperand.TRUE;
import static com.plasstech.lang.d2.optimize.OpcodeSubject.assertThat;
import static org.junit.Assert.assertThrows;

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
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.testing.TestUtils;
import com.plasstech.lang.d2.type.ArrayType;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.testing.IntegralTypeProvider;
import com.plasstech.lang.d2.type.testing.NumericTypeProvider;

@RunWith(TestParameterInjector.class)
public class ArithmeticOptimizerTest {
  private final static Optimizer OPTIMIZER = new ArithmeticOptimizer(2);

  private static final TempLocation INT1 = LocationUtils.newTempLocation("int1", VarType.INT);
  private static final TempLocation INT2 = LocationUtils.newTempLocation("int2", VarType.INT);
  private static final TempLocation STRING_TEMP =
      LocationUtils.newTempLocation("temp3", VarType.STRING);
  private static final TempLocation DBL1 = LocationUtils.newTempLocation("dbl1", VarType.DOUBLE);
  private static final ConstantOperand<String> CONSTANT_A = ConstantOperand.of("a");
  private static final ConstantOperand<String> CONSTANT_B = ConstantOperand.of("b");
  private static final ConstantOperand<String> NULL_STRING =
      new ConstantOperand<String>(null, VarType.STRING);

  @Test
  public void varPlusVarBecomesShift() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, INT2, TokenType.PLUS, INT2, null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isBinOp(INT1, INT2, TokenType.SHIFT_LEFT, ONE);
  }

  @Test
  public void constPlusConst(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DBL1, one, TokenType.PLUS, one, null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(2, varType));
  }

  @Test
  public void doubleConstPlusConst() {

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DBL1, ConstantOperand.of(1.23), TokenType.PLUS,
            ConstantOperand.of(234.56), null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(235.79));
  }

  @Test
  public void constMinusConst(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DBL1, one, TokenType.MINUS, one, null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(0, varType));
  }

  @Test
  public void constMultConst(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DBL1, ConstantOperand.fromValue(2, varType), TokenType.MULT,
            ConstantOperand.fromValue(3, varType), null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(6, varType));
  }

  @Test
  public void constDivConst(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DBL1, ConstantOperand.fromValue(12, varType), TokenType.DIV,
            ConstantOperand.fromValue(3, varType), null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(4, varType));
  }

  @Test
  public void constDivConstLessThan1(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DBL1, ConstantOperand.fromValue(1, varType), TokenType.DIV,
            ConstantOperand.fromValue(6, varType), null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(0, varType));
  }

  @Test
  public void constDivConstNegativeLessThan1(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DBL1, ConstantOperand.fromValue(-1, varType), TokenType.DIV,
            ConstantOperand.fromValue(-6, varType), null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
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

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
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

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
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
    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);
    assertThat(optimized.get(0)).isTransferredFrom(zero);
  }

  @Test
  public void varPlusVarStrings() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, STRING_TEMP, TokenType.PLUS, STRING_TEMP, null));
    OPTIMIZER.optimize(program, null);
    assertThat(OPTIMIZER.isChanged()).isFalse();
  }

  @Test
  public void varPlusEmptyStringRight() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, STRING_TEMP, TokenType.PLUS, EMPTY_STRING, null));
    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized.get(0)).isTransferredFrom(STRING_TEMP);
  }

  @Test
  public void varPlusEmptyStringLeft() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, EMPTY_STRING, TokenType.PLUS, STRING_TEMP, null));
    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized.get(0)).isTransferredFrom(STRING_TEMP);
  }

  @Test
  public void varStringPlusNull() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, STRING_TEMP, TokenType.PLUS, NULL_STRING, null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> OPTIMIZER.optimize(program, null));
    assertThat(exception).hasMessageThat().contains("Cannot add NULL to STRING");
  }

  @Test
  public void constStringPlusNull() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT1, CONSTANT_A, TokenType.PLUS, NULL_STRING, null));
    RuntimeException exception =
        assertThrows(D2RuntimeException.class, () -> OPTIMIZER.optimize(program, null));
    assertThat(exception).hasMessageThat().contains("Cannot add NULL to STRING");
  }

  @Test
  public void constStringPlusConstString(
      @TestParameter({"a", ""}) String leftValue,
      @TestParameter({"", "b"}) String rightValue) {

    Operand left = ConstantOperand.of(leftValue);
    Operand right = ConstantOperand.of(rightValue);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(STRING_TEMP, left, TokenType.PLUS, right, null));
    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
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

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(6);
    assertThat(optimized.get(0)).isTransferredFrom(TRUE);
    assertThat(optimized.get(1)).isTransferredFrom(FALSE);
    assertThat(optimized.get(2)).isTransferredFrom(TRUE);
    assertThat(optimized.get(3)).isTransferredFrom(TRUE);
    assertThat(optimized.get(4)).isTransferredFrom(FALSE);
    assertThat(optimized.get(5)).isTransferredFrom(FALSE);
  }

  @Test
  public void eqConstant(
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);
    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(INT1, one, TokenType.EQEQ, one, null),
            new BinOp(INT1, zero, TokenType.EQEQ, one, null),
            new BinOp(INT1, one, TokenType.NEQ, one, null),
            new BinOp(INT1, zero, TokenType.NEQ, one, null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
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
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(DBL1, one, TokenType.GT, zero, null),
            new BinOp(DBL1, zero, TokenType.GT, one, null),
            new BinOp(DBL1, one, TokenType.LT, zero, null),
            new BinOp(DBL1, zero, TokenType.LT, one, null),
            new BinOp(DBL1, one, TokenType.LEQ, zero, null),
            new BinOp(DBL1, zero, TokenType.LEQ, one, null),
            new BinOp(DBL1, one, TokenType.GEQ, zero, null),
            new BinOp(DBL1, zero, TokenType.GEQ, one, null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
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

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
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

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(4);
    assertThat(optimized.get(0)).isTransferredFrom(TRUE);
    assertThat(optimized.get(1)).isTransferredFrom(FALSE);
    assertThat(optimized.get(2)).isTransferredFrom(FALSE);
    assertThat(optimized.get(3)).isTransferredFrom(TRUE);
  }

  @Test
  public void bitOperations(@TestParameter({"&", "|", "^"}) String operation) {
    // TODO: don't use the interpeter here
    TestUtils.optimizeAssertSameVariables(String.format("b=111 %s 4 println b", operation),
        OPTIMIZER);
  }

  @Test
  public void stringOperationsGlobals() {
    // TODO: don't use the interpeter here
    TestUtils.optimizeAssertSameVariables(
        "a='123'[0] b=length('123') c=chr(65) d=asc('a') println a println b println c println d",
        OPTIMIZER);
  }

  @Test
  public void stringOperations() {
    // TODO: don't use the interpeter here
    TestUtils.optimizeAssertSameVariables(
        "p:proc {s='123' a=s[0] b=length(s) c=asc(a) d=chr(c) println s println a println b println d}",
        OPTIMIZER);
  }

  @Test
  public void stringIndexOutOfRange() {
    ImmutableList<Op> program =
        ImmutableList.of(
            // Previously, it was only testing if index was > length, but it needed to be >=
            new BinOp(STRING_TEMP, CONSTANT_A, TokenType.LBRACKET, ConstantOperand.of(1), null));

    assertThrows(D2RuntimeException.class, () -> OPTIMIZER.optimize(program, null));
  }

  @Test
  public void boolOperations(
      @TestParameter({"true", "false"}) boolean left,
      @TestParameter({"true", "false"}) boolean right,
      @TestParameter({"AND", "OR", "XOR"}) TokenType operator) {

    // TODO: don't use the interpeter here
    TestUtils.optimizeAssertSameVariables(
        String.format("a=%s %s %s", left, operator, right), OPTIMIZER);
  }

  @Test
  public void modItself(
      @TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    Location left = LocationUtils.newParamLocation("operand", varType, 0, 0);
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(left, left, TokenType.MOD, left, null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.fromValue(0, varType));
  }

  @Test
  public void mod1(@TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(INT2, ConstantOperand.fromValue(14, varType), TokenType.MOD,
            ConstantOperand.fromValue(1, varType), null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
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

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(dest, left, operand, one, null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(left);
  }

  @Test
  public void opZeroUnchanged(
      @TestParameter(
        {"PLUS", "MINUS", "SHIFT_LEFT", "SHIFT_RIGHT", "BIT_XOR", "BIT_OR"}
      ) TokenType operand,
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    assume().that(
        (operand == TokenType.SHIFT_LEFT
            || operand == TokenType.BIT_XOR
            || operand == TokenType.SHIFT_RIGHT
            || operand == TokenType.BIT_OR)
            && !varType.isIntegral())
        .isFalse();

    Location dest = LocationUtils.newTempLocation("dest", varType);
    Operand left = LocationUtils.newTempLocation("operand", varType);
    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(dest, left, operand, zero, null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(left);
  }

  @Test
  public void unaryPlusMinus(
      @TestParameter({"PLUS", "MINUS"}) TokenType operand,
      @TestParameter(valuesProvider = NumericTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(INT1, operand, one, null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    if (operand == TokenType.PLUS) {
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

    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(dest, left, operator, zero, null));

    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(zero);
  }

  @Test
  public void constantArrayLength() {
    ArrayType arrayType = new ArrayType(VarType.INT, 1).setKnownLength(3);
    Operand constArray = LocationUtils.newMemoryAddress("array", arrayType);
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(INT1, TokenType.LENGTH, constArray, null));
    ImmutableList<Op> optimized = OPTIMIZER.optimize(program, null);

    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.of(3));
  }

  @Test
  public void variableArrayLength() {
    ArrayType arrayType = new ArrayType(VarType.INT, 1);
    Operand constArray = LocationUtils.newMemoryAddress("array", arrayType);
    ImmutableList<Op> program =
        ImmutableList.of(new UnaryOp(INT1, TokenType.LENGTH, constArray, null));

    OPTIMIZER.optimize(program, null);

    assertThat(OPTIMIZER.isChanged()).isFalse();
  }
}
