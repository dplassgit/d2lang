package com.plasstech.lang.d2.codegen;

import java.util.List;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.ExecutionEnvironment;
import com.plasstech.lang.d2.codegen.il.Op;

public class ILOptimizerTest {

  @Test
  public void plusZero() {
    TestUtils.optimizeAssertSameVariables("a = 0 + 1 b = a + 0 c = 2 + 0");
  }

  @Test
  public void plusConstants() {
    TestUtils.optimizeAssertSameVariables("a = 2 + 3");
  }

  @Test
  public void minusConstants() {
    TestUtils.optimizeAssertSameVariables("a = 2 - 3");
  }

  @Test
  public void divConstants() {
    TestUtils.optimizeAssertSameVariables("a = 10 / 2");
  }

  @Test
  public void divByZero() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 1 / 0");
    try {
      ee.execute();
    } catch (Exception e) {
    }
    System.out.println(Joiner.on("\n").join(ee.ilCode()));

    List<Op> optimized = new ILOptimizer().optimize(ee.ilCode());
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    try {
      ee.execute(optimized);
    } catch (Exception e) {
    }
  }

  @Test
  public void modConstants() {
    TestUtils.optimizeAssertSameVariables("a = 14 % 5");
  }

  @Test
  public void plusStringConstants() {
    TestUtils.optimizeAssertSameVariables("a = 'hi' + ' there'");
  }

  @Test
  public void timesZero() {
    TestUtils.optimizeAssertSameVariables("a = 1 * 0 b = a * 0 d = 3 * 0");
  }

  @Test
  public void timesConstants() {
    TestUtils.optimizeAssertSameVariables("a = 2 * 3");
  }

  @Test
  public void andConstant() {
    TestUtils.optimizeAssertSameVariables("a = true b = a & true");
    TestUtils.optimizeAssertSameVariables("a = true b = true and a");
    TestUtils.optimizeAssertSameVariables("a = true and true");
    TestUtils.optimizeAssertSameVariables("a = true & false");
    TestUtils.optimizeAssertSameVariables("a = false and true");
    TestUtils.optimizeAssertSameVariables("a = false and false");
  }

  @Test
  public void orConstant() {
    TestUtils.optimizeAssertSameVariables("a = true b = a | true");
    TestUtils.optimizeAssertSameVariables("a = true b = true or a");
    TestUtils.optimizeAssertSameVariables("a = true or true");
    TestUtils.optimizeAssertSameVariables("a = true or false");
    TestUtils.optimizeAssertSameVariables("a = false | true");
    TestUtils.optimizeAssertSameVariables("a = false or false");
  }

  @Test
  public void eqIntConstant() {
    TestUtils.optimizeAssertSameVariables("a = 3==3");
    TestUtils.optimizeAssertSameVariables("a = 4==3");
    TestUtils.optimizeAssertSameVariables("a = 3!=3");
    TestUtils.optimizeAssertSameVariables("a = 4!=3");
  }

  @Test
  public void eqStringConstant() {
    TestUtils.optimizeAssertSameVariables("a = 'hi' == 'bye'");
    TestUtils.optimizeAssertSameVariables("a = 'hi' == 'hi'");
    TestUtils.optimizeAssertSameVariables("a = 'hi' != 'bye'");
    TestUtils.optimizeAssertSameVariables("a = 'hi' != 'hi'");
  }

  @Test
  public void eqBoolConstant() {
    TestUtils.optimizeAssertSameVariables("a = true == true");
    TestUtils.optimizeAssertSameVariables("a = true != true");
    TestUtils.optimizeAssertSameVariables("a = true == false");
    TestUtils.optimizeAssertSameVariables("a = true != false");
    TestUtils.optimizeAssertSameVariables("a = false == false");
    TestUtils.optimizeAssertSameVariables("a = false != false");
  }

  @Test
  public void ltGtIntConstant() {
    TestUtils.optimizeAssertSameVariables("a = 3>3");
    TestUtils.optimizeAssertSameVariables("a = 4>=3");
    TestUtils.optimizeAssertSameVariables("a = 3<3");
    TestUtils.optimizeAssertSameVariables("a = 4<=3");
  }

  @Test
  public void constantPropagationTransfer() {
    TestUtils.optimizeAssertSameVariables("a = 4 b = a");
  }

  @Test
  public void constantAsc() {
    TestUtils.optimizeAssertSameVariables("a = asc('b') b = a");
  }

  @Test
  public void constantChr() {
    TestUtils.optimizeAssertSameVariables("a = chr(65) b = a");
  }

  @Test
  public void constantStringLength() {
    TestUtils.optimizeAssertSameVariables("a = length('abc') b = a");
  }

  @Test
  public void constantArrayLength() {
    TestUtils.optimizeAssertSameVariables("a = length([1,2,3,4]) b = a");
    TestUtils.optimizeAssertSameVariables("a = length([true, false]) b = a");
    TestUtils.optimizeAssertSameVariables("a = length(['a', 'b', 'c']) b = a");
  }

  @Test
  public void constantPropIf() {
    TestUtils.optimizeAssertSameVariables("a = 4 if a ==3 { print a}");
  }

  @Test
  public void constantPropReturn() {
    TestUtils.optimizeAssertSameVariables("a:proc():int { return 3} print a()");
  }

  @Test
  public void constantPropCall() {
    TestUtils.optimizeAssertSameVariables(
        "a:proc(n:int, m:int):int { return n+1} b=4 print a(4, b) print a(b+2, 4+6)");
  }

  @Test
  public void multipleReturns() {
    TestUtils.optimizeAssertSameVariables(
        "toString: proc(i: int): string {\r\n"
            + "  if i == 0 {\r\n"
            + "    return '0'\r\n"
            + "  }\r\n"
            + "  val = ''\r\n"
            + "  return val\r\n"
            + "}"
            + "  println toString(314159)\r\n");
  }
}
