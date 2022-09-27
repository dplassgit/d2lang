package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.codegen.ConstantOperand.EMPTY_STRING;
import static com.plasstech.lang.d2.codegen.ConstantOperand.FALSE;
import static com.plasstech.lang.d2.codegen.ConstantOperand.ONE;
import static com.plasstech.lang.d2.codegen.ConstantOperand.ONE_DBL;
import static com.plasstech.lang.d2.codegen.ConstantOperand.TRUE;
import static com.plasstech.lang.d2.codegen.ConstantOperand.ZERO;
import static com.plasstech.lang.d2.codegen.ConstantOperand.ZERO_DBL;
import static com.plasstech.lang.d2.optimize.OptimizerAsserts.assertTransferFrom;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Operand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.testing.TestUtils;
import com.plasstech.lang.d2.type.VarType;

@RunWith(TestParameterInjector.class)
public class ArithmeticOptimizerTest {
  private static final Operand TWO_DBL = ConstantOperand.of(2.0);
  private final Optimizer optimizer = new ArithmeticOptimizer(2);
  private final Optimizer OPTIMIZERS =
      new ILOptimizer(
              ImmutableList.of(optimizer, new ConstantPropagationOptimizer(0), new NopOptimizer()))
          .setDebugLevel(2);

  private static final TempLocation TEMP1 = new TempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = new TempLocation("temp2", VarType.INT);
  private static final TempLocation STRING_TEMP = new TempLocation("temp3", VarType.STRING);
  private static final TempLocation DBL1 = new TempLocation("temp1", VarType.DOUBLE);
  private static final ConstantOperand<String> CONSTANT_A = ConstantOperand.of("a");
  private static final ConstantOperand<String> CONSTANT_B = ConstantOperand.of("b");

  @Test
  public void varPlusVarInts() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP1, TEMP2, TokenType.PLUS, TEMP2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.left()).isEqualTo(TEMP2);
    assertThat(first.operator()).isEqualTo(TokenType.SHIFT_LEFT);
    assertThat(first.right()).isEqualTo(ONE);
  }

  @Test
  public void constPlusVar_swaps() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ConstantOperand.ONE, TokenType.PLUS, TEMP1, null),
            new BinOp(TEMP2, ConstantOperand.ONE, TokenType.MULT, TEMP2, new Position(0, 0)));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);

    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.left()).isEqualTo(TEMP1);
    assertThat(first.operator()).isEqualTo(TokenType.PLUS);
    assertThat(first.right()).isEqualTo(ONE);

    BinOp second = (BinOp) optimized.get(1);
    assertThat(second.left()).isEqualTo(TEMP2);
    assertThat(second.operator()).isEqualTo(TokenType.MULT);
    assertThat(second.right()).isEqualTo(ONE);
  }

  @Test
  public void varPlusVarDouble() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DBL1, ONE_DBL, TokenType.PLUS, ONE_DBL, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertTransferFrom(optimized.get(0), TWO_DBL);
  }

  @Test
  public void varMinusVarDouble() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DBL1, ONE_DBL, TokenType.MINUS, ONE_DBL, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertTransferFrom(optimized.get(0), ZERO_DBL);
  }

  @Test
  public void varMultVarDouble() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(DBL1, ONE_DBL, TokenType.MULT, TWO_DBL, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    assertTransferFrom(optimized.get(0), TWO_DBL);
  }

  @Test
  public void varDivVarDouble() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(DBL1, TWO_DBL, TokenType.DIV, ONE_DBL, null),
            new BinOp(DBL1, TWO_DBL, TokenType.DIV, TWO_DBL, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);

    assertTransferFrom(optimized.get(0), TWO_DBL);
    assertTransferFrom(optimized.get(1), ONE_DBL);
  }

  @Test
  public void varPlusVarStrings() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP1, STRING_TEMP, TokenType.PLUS, STRING_TEMP, null));
    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void varPlusEmptyStringRight() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP1, STRING_TEMP, TokenType.PLUS, EMPTY_STRING, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertTransferFrom(optimized.get(0), STRING_TEMP);
  }

  @Test
  public void varPlusEmptyStringLeft() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP1, EMPTY_STRING, TokenType.PLUS, STRING_TEMP, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    assertTransferFrom(optimized.get(0), STRING_TEMP);
  }

  @Test
  public void compareConstantInts() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ONE, TokenType.LEQ, ZERO, null),
            new BinOp(TEMP1, ZERO, TokenType.LEQ, ONE, null),
            new BinOp(TEMP1, ONE, TokenType.GEQ, ZERO, null),
            new BinOp(TEMP1, ZERO, TokenType.GEQ, ONE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(4);
    assertTransferFrom(optimized.get(0), FALSE);
    assertTransferFrom(optimized.get(1), TRUE);
    assertTransferFrom(optimized.get(2), TRUE);
    assertTransferFrom(optimized.get(3), FALSE);
  }

  @Test
  public void compareObjectsLeqGeq() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, TEMP2, TokenType.LEQ, TEMP2, null),
            new BinOp(TEMP1, TEMP2, TokenType.GEQ, TEMP2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    assertTransferFrom(optimized.get(0), TRUE);
    assertTransferFrom(optimized.get(1), TRUE);
  }

  @Test
  public void compareSameObjectsLtGt() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, TEMP2, TokenType.LT, TEMP2, null),
            new BinOp(TEMP1, TEMP2, TokenType.GT, TEMP2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    // we know that temp2 is not < or > itself
    assertTransferFrom(optimized.get(0), FALSE);
    assertTransferFrom(optimized.get(1), FALSE);
  }

  @Test
  public void compareObjectsEq() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, TEMP2, TokenType.EQEQ, TEMP2, null),
            new BinOp(TEMP1, TEMP2, TokenType.NEQ, TEMP2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    assertTransferFrom(optimized.get(0), TRUE);
    assertTransferFrom(optimized.get(1), FALSE);
  }

  @Test
  public void compareIntsGt() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ONE, TokenType.GT, ZERO, null),
            new BinOp(TEMP1, ZERO, TokenType.GT, ONE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    assertTransferFrom(optimized.get(0), TRUE);
    assertTransferFrom(optimized.get(1), FALSE);
  }

  @Test
  public void compareDoubles() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(DBL1, ONE_DBL, TokenType.GT, ZERO_DBL, null),
            new BinOp(DBL1, ZERO_DBL, TokenType.GT, ONE_DBL, null),
            new BinOp(DBL1, ONE_DBL, TokenType.LT, ZERO_DBL, null),
            new BinOp(DBL1, ZERO_DBL, TokenType.LT, ONE_DBL, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(4);
    assertTransferFrom(optimized.get(0), TRUE);
    assertTransferFrom(optimized.get(1), FALSE);
    assertTransferFrom(optimized.get(2), FALSE);
    assertTransferFrom(optimized.get(3), TRUE);
  }

  @Test
  public void compareConstantStringsLeqGeq() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, CONSTANT_B, TokenType.LEQ, CONSTANT_A, null),
            new BinOp(TEMP1, CONSTANT_A, TokenType.LEQ, CONSTANT_B, null),
            new BinOp(TEMP1, CONSTANT_B, TokenType.GEQ, CONSTANT_A, null),
            new BinOp(TEMP1, CONSTANT_A, TokenType.GEQ, CONSTANT_B, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(4);
    assertTransferFrom(optimized.get(0), FALSE);
    assertTransferFrom(optimized.get(1), TRUE);
    assertTransferFrom(optimized.get(2), TRUE);
    assertTransferFrom(optimized.get(3), FALSE);
  }

  @Test
  public void compareConstantStringsGtLt() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, CONSTANT_B, TokenType.GT, CONSTANT_A, null),
            new BinOp(TEMP1, CONSTANT_A, TokenType.GT, CONSTANT_B, null),
            new BinOp(TEMP1, CONSTANT_B, TokenType.LT, CONSTANT_A, null),
            new BinOp(TEMP1, CONSTANT_A, TokenType.LT, CONSTANT_B, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(4);
    assertTransferFrom(optimized.get(0), TRUE);
    assertTransferFrom(optimized.get(1), FALSE);
    assertTransferFrom(optimized.get(2), FALSE);
    assertTransferFrom(optimized.get(3), TRUE);
  }

  @Test
  public void bitOperations(@TestParameter({"&", "|", "^"}) String operation) {
    TestUtils.optimizeAssertSameVariables(String.format("b=111%s4 d=!111", operation), OPTIMIZERS);
  }

  @Test
  public void stringOperationsGlobals() {
    TestUtils.optimizeAssertSameVariables(
        "a='123'[0] b=length('123') c=chr(65) d=asc('a')", OPTIMIZERS);
  }

  @Test
  public void stringOperations() {
    TestUtils.optimizeAssertSameVariables(
        "p:proc {s='123' a=s[0] b=length(s) c=asc(a) d=chr(c)}", OPTIMIZERS);
  }

  @Test
  public void printConstantInt() {
    InterpreterResult result = TestUtils.optimizeAssertSameVariables("print 3", OPTIMIZERS);

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo("3");
  }

  @Test
  public void printConstantBool(@TestParameter boolean val) {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables(String.format("print %s", val), OPTIMIZERS);

    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<?> arg = (ConstantOperand<?>) first.arg();
    assertThat(arg.value()).isEqualTo(String.valueOf(val));
  }

  @Test
  public void boolOperations(
      @TestParameter({"true", "false"}) String left,
      @TestParameter({"true", "false"}) String right,
      @TestParameter({"and", "or", "xor"}) String operator) {

    TestUtils.optimizeAssertSameVariables(
        String.format("a=%s %s %s", left, operator, right), OPTIMIZERS);
  }

}
