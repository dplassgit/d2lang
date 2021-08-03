package com.plasstech.lang.d2.optimize;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.testing.TestUtils;

public class ArithmeticOptimizerTest {
  private ArithmeticOptimizer optimizer = new ArithmeticOptimizer(0);

  @Test
  public void bitOperations() {
    TestUtils.optimizeAssertSameVariables("b=111&4 c=111|20 d=!111 e=111^4 ", optimizer);
  }

  @Test
  public void boolOperations() {
    for (String bool1 : ImmutableList.of("true", "false")) {
      // this is optimized in the parser
      // TestUtils.optimizeAssertSameVariables(String.format("a=not %s", bool1), optimizer);
      for (String bool2 : ImmutableList.of("true", "false")) {
        for (String op : ImmutableList.of("and", "or", "xor")) {
          TestUtils.optimizeAssertSameVariables(
              String.format("a=%s %s %s", bool1, op, bool2), optimizer);
        }
      }
    }
  }
}
