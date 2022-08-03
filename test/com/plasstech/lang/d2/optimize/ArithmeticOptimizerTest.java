package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.testing.TestUtils;
import com.plasstech.lang.d2.type.VarType;

@RunWith(TestParameterInjector.class)
public class ArithmeticOptimizerTest {
  private final Optimizer optimizer = new ArithmeticOptimizer(2);
  private final ILOptimizer OPTIMIZERS =
      new ILOptimizer(
              ImmutableList.of(optimizer, new ConstantPropagationOptimizer(0), new NopOptimizer()))
          .setDebugLevel(2);

  private static final TempLocation TEMP1 = new TempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = new TempLocation("temp2", VarType.INT);
  private static final TempLocation TEMP3 = new TempLocation("temp3", VarType.STRING);
  private static final TempLocation DBL1 = new TempLocation("temp1", VarType.DOUBLE);

  // TODO: write more "simple" tests like this.
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
    assertThat(first.right()).isEqualTo(ConstantOperand.ONE);
  }

  @Test
  public void varPlusVarDouble() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                DBL1, ConstantOperand.ONE_DBL, TokenType.PLUS, ConstantOperand.ONE_DBL, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(ConstantOperand.of(2.0));
  }

  @Test
  public void varMinusVarDouble() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                DBL1, ConstantOperand.ONE_DBL, TokenType.MINUS, ConstantOperand.ONE_DBL, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(ConstantOperand.ZERO_DBL);
  }

  @Test
  public void varMultVarDouble() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                DBL1, ConstantOperand.ONE_DBL, TokenType.MULT, ConstantOperand.ONE_DBL, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(ConstantOperand.ONE_DBL);
  }

  @Test
  public void varDivVarDouble() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(DBL1, ConstantOperand.ONE_DBL, TokenType.DIV, ConstantOperand.ONE_DBL, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);

    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(ConstantOperand.ONE_DBL);
  }

  @Test
  public void varPlusVarStrings() {
    ImmutableList<Op> program =
        ImmutableList.of(new BinOp(TEMP1, TEMP3, TokenType.PLUS, TEMP3, null));
    optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void varPlusEmptyStringRight() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, TEMP3, TokenType.PLUS, ConstantOperand.EMPTY_STRING, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(TEMP3);
  }

  @Test
  public void varPlusEmptyStringLeft() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ConstantOperand.EMPTY_STRING, TokenType.PLUS, TEMP3, null));
    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimizer.isChanged()).isTrue();

    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(TEMP3);
  }

  @Test
  public void compareIntsLeq() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ConstantOperand.ONE, TokenType.LEQ, ConstantOperand.ZERO, null),
            new BinOp(TEMP1, ConstantOperand.ZERO, TokenType.LEQ, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(ConstantOperand.FALSE);
    Transfer second = (Transfer) optimized.get(1);
    assertThat(second.source()).isEqualTo(ConstantOperand.TRUE);
  }

  @Test
  public void compareIntsGt() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ConstantOperand.ONE, TokenType.GT, ConstantOperand.ZERO, null),
            new BinOp(TEMP1, ConstantOperand.ZERO, TokenType.GT, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(ConstantOperand.TRUE);
    Transfer second = (Transfer) optimized.get(1);
    assertThat(second.source()).isEqualTo(ConstantOperand.FALSE);
  }

  @Test
  public void compareDuoublesGt() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(DBL1, ConstantOperand.ONE_DBL, TokenType.GT, ConstantOperand.ZERO_DBL, null),
            new BinOp(DBL1, ConstantOperand.ZERO_DBL, TokenType.GT, ConstantOperand.ONE_DBL, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(ConstantOperand.TRUE);
    Transfer second = (Transfer) optimized.get(1);
    assertThat(second.source()).isEqualTo(ConstantOperand.FALSE);
  }

  @Test
  public void compareStringsLeq() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ConstantOperand.of("b"), TokenType.LEQ, ConstantOperand.of("a"), null),
            new BinOp(
                TEMP1, ConstantOperand.of("a"), TokenType.LEQ, ConstantOperand.of("b"), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(ConstantOperand.FALSE);
    Transfer second = (Transfer) optimized.get(1);
    assertThat(second.source()).isEqualTo(ConstantOperand.TRUE);
  }

  @Test
  public void compareStringsGe() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(TEMP1, ConstantOperand.of("b"), TokenType.GT, ConstantOperand.of("a"), null),
            new BinOp(TEMP1, ConstantOperand.of("a"), TokenType.GT, ConstantOperand.of("b"), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    Transfer first = (Transfer) optimized.get(0);
    assertThat(first.source()).isEqualTo(ConstantOperand.TRUE);
    Transfer second = (Transfer) optimized.get(1);
    assertThat(second.source()).isEqualTo(ConstantOperand.FALSE);
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
    ConstantOperand<String> arg = (ConstantOperand<String>) first.arg();
    assertThat(arg.value()).isEqualTo("3");
  }

  @Test
  public void printConstantBool(@TestParameter boolean val) {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables(String.format("print %s", val), OPTIMIZERS);
    ImmutableList<Op> code = result.code();
    SysCall first = (SysCall) code.get(0);
    ConstantOperand<String> arg = (ConstantOperand<String>) first.arg();
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
