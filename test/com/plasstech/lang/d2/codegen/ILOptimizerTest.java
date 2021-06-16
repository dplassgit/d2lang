package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.ExecutionEnvironment;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.Environment;

public class ILOptimizerTest {

  @Test
  public void plusZero() {
    optimizeAssertSameVariables("a = 0 + 1 b = a + 0 c = 2 + 0");
  }

  @Test
  public void plusConstants() {
    optimizeAssertSameVariables("a = 2 + 3");
  }

  @Test
  public void minusConstants() {
    optimizeAssertSameVariables("a = 2 - 3");
  }

  @Test
  public void divConstants() {
    optimizeAssertSameVariables("a = 10 / 2");
  }

  @Test
  public void divByZero() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 1 / 0");
    try {
      ee.execute();
    } catch (Exception e) {
    }
    System.out.println(Joiner.on("\n").join(ee.ilCode()));

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    try {
      ee.execute(optimized);
    } catch (Exception e) {
    }
  }

  @Test
  public void modConstants() {
    optimizeAssertSameVariables("a = 14 % 5");
  }

  @Test
  public void plusStringConstants() {
    optimizeAssertSameVariables("a = 'hi' + ' there'");
  }

  @Test
  public void timesZero() {
    optimizeAssertSameVariables("a = 1 * 0 b = a * 0 d = 3 * 0");
  }

  @Test
  public void timesConstants() {
    optimizeAssertSameVariables("a = 2 * 3");
  }

  @Test
  public void andConstant() {
    optimizeAssertSameVariables("a = true b = a & true");
    optimizeAssertSameVariables("a = true b = true and a");
    optimizeAssertSameVariables("a = true and true");
    optimizeAssertSameVariables("a = true & false");
    optimizeAssertSameVariables("a = false and true");
    optimizeAssertSameVariables("a = false and false");
  }

  @Test
  public void orConstant() {
    optimizeAssertSameVariables("a = true b = a | true");
    optimizeAssertSameVariables("a = true b = true or a");
    optimizeAssertSameVariables("a = true or true");
    optimizeAssertSameVariables("a = true or false");
    optimizeAssertSameVariables("a = false | true");
    optimizeAssertSameVariables("a = false or false");
  }

  @Test
  public void eqIntConstant() {
    optimizeAssertSameVariables("a = 3==3");
    optimizeAssertSameVariables("a = 4==3");
    optimizeAssertSameVariables("a = 3!=3");
    optimizeAssertSameVariables("a = 4!=3");
  }

  @Test
  public void eqStringConstant() {
    optimizeAssertSameVariables("a = 'hi' == 'bye'");
    optimizeAssertSameVariables("a = 'hi' == 'hi'");
    optimizeAssertSameVariables("a = 'hi' != 'bye'");
    optimizeAssertSameVariables("a = 'hi' != 'hi'");
  }

  @Test
  public void eqBoolConstant() {
    optimizeAssertSameVariables("a = true == true");
    optimizeAssertSameVariables("a = true != true");
    optimizeAssertSameVariables("a = true == false");
    optimizeAssertSameVariables("a = true != false");
    optimizeAssertSameVariables("a = false == false");
    optimizeAssertSameVariables("a = false != false");
  }

  @Test
  public void ltGtIntConstant() {
    optimizeAssertSameVariables("a = 3>3");
    optimizeAssertSameVariables("a = 4>=3");
    optimizeAssertSameVariables("a = 3<3");
    optimizeAssertSameVariables("a = 4<=3");
  }

  @Test
  public void constantPropagationTransfer() {
    optimizeAssertSameVariables("a = 4 b = a");
  }

  @Test
  public void constantAsc() {
    optimizeAssertSameVariables("a = asc('b') b = a");
  }

  @Test
  public void constantChr() {
    optimizeAssertSameVariables("a = chr(65) b = a");
  }

  @Test
  public void constantStringLength() {
    optimizeAssertSameVariables("a = length('abc') b = a");
  }

  @Test
  public void constantArrayLength() {
    optimizeAssertSameVariables("a = length([1,2,3,4]) b = a");
    optimizeAssertSameVariables("a = length([true, false]) b = a");
    optimizeAssertSameVariables("a = length(['a', 'b', 'c']) b = a");
  }

  @Test
  public void constantPropIf() {
    optimizeAssertSameVariables("a = 4 if a ==3 { print a}");
  }

  @Test
  public void constantPropReturn() {
    optimizeAssertSameVariables("a:proc():int { return 3} print a()");
  }

  @Test
  public void constantPropCall() {
    optimizeAssertSameVariables(
        "a:proc(n:int, m:int):int { return n+1} b=4 print a(4, b) print a(b+2, 4+6)");
  }

  private void optimizeAssertSameVariables(String program) {
    ExecutionEnvironment ee = new ExecutionEnvironment(program);
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));

    List<Op> originalCode = new ArrayList<>(ee.ilCode());

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    Environment env2 = ee.execute(optimized);
    assertThat(env2.variables()).isEqualTo(env.variables());
    assertThat(env2.output()).isEqualTo(env.output());

    assertWithMessage("Should have made at least one optimization")
        .that(originalCode)
        .isNotEqualTo(optimized);
  }
}
