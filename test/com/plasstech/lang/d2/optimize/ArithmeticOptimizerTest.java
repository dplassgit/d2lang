package com.plasstech.lang.d2.optimize;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.testing.TestUtils;

public class ArithmeticOptimizerTest {
  private static final ILOptimizer OPTIMIZER =
      new ILOptimizer(
              ImmutableList.of(new ArithmeticOptimizer(2), new ConstantPropagationOptimizer(0)))
          .setDebugLevel(2);

  @Test
  public void bitOperations() {
    TestUtils.optimizeAssertSameVariables("b=111&4 c=111|20 d=!111 e=111^4 ", OPTIMIZER);
  }

  @Test
  public void stringOperationsGlobals() {
    TestUtils.optimizeAssertSameVariables(
        "a='123'[0] b=length('123') c=chr(65) d=asc('a')", OPTIMIZER);
  }

  @Test
  public void stringOperations() {
    TestUtils.optimizeAssertSameVariables(
        "p:proc {s='123' a=s[0] b=length(s) c=asc(a) d=chr(c)}", OPTIMIZER);
  }

  @Test
  public void boolOperations() {
    for (String bool1 : ImmutableList.of("true", "false")) {
      // this is optimized in the parser
      // TestUtils.optimizeAssertSameVariables(String.format("a=not %s", bool1), optimizer);
      for (String bool2 : ImmutableList.of("true", "false")) {
        for (String op : ImmutableList.of("and", "or", "xor")) {
          TestUtils.optimizeAssertSameVariables(
              String.format("a=%s %s %s", bool1, op, bool2), OPTIMIZER);
        }
      }
    }
  }
}
