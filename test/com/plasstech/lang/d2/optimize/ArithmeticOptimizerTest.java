package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.testing.TestUtils;
import com.plasstech.lang.d2.type.VarType;

public class ArithmeticOptimizerTest {
  private static final ILOptimizer OPTIMIZERS =
      new ILOptimizer(
              ImmutableList.of(new ArithmeticOptimizer(2), new ConstantPropagationOptimizer(0)))
          .setDebugLevel(2);

  private static final TempLocation TEMP1 = new TempLocation("temp1", VarType.INT);
  private static final TempLocation TEMP2 = new TempLocation("temp2", VarType.INT);
  private static final TempLocation TEMP3 = new TempLocation("temp3", VarType.STRING);

  // TODO: write more "simple" tests like this.
  @Test
  public void varPlusVarInts() {
    ImmutableList<Op> program = ImmutableList.of(new BinOp(TEMP1, TEMP2, TokenType.PLUS, TEMP2));
    Optimizer optimizer = new ArithmeticOptimizer(2);
    ImmutableList<Op> optimized = optimizer.optimize(program);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(1);
    BinOp first = (BinOp) optimized.get(0);
    assertThat(first.left()).isEqualTo(TEMP2);
    assertThat(first.operator()).isEqualTo(TokenType.SHIFT_LEFT);
    assertThat(first.right()).isEqualTo(ConstantOperand.ONE);
  }

  @Test
  public void varPlusVarStrings() {
    ImmutableList<Op> program = ImmutableList.of(new BinOp(TEMP1, TEMP3, TokenType.PLUS, TEMP3));
    Optimizer optimizer = new ArithmeticOptimizer(2);
    optimizer.optimize(program);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void bitOperations() {
    TestUtils.optimizeAssertSameVariables("b=111&4 c=111|20 d=!111 e=111^4 ", OPTIMIZERS);
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
  public void boolOperations() {
    for (String bool1 : ImmutableList.of("true", "false")) {
      // this is optimized in the parser
      // TestUtils.optimizeAssertSameVariables(String.format("a=not %s", bool1), optimizer);
      for (String bool2 : ImmutableList.of("true", "false")) {
        for (String op : ImmutableList.of("and", "or", "xor")) {
          TestUtils.optimizeAssertSameVariables(
              String.format("a=%s %s %s", bool1, op, bool2), OPTIMIZERS);
        }
      }
    }
  }
}
