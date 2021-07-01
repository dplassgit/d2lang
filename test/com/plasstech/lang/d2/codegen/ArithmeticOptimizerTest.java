package com.plasstech.lang.d2.codegen;

import org.junit.Test;

public class ArithmeticOptimizerTest {
  private ArithmeticOptimizer optimizer = new ArithmeticOptimizer(2);

  @Test
  public void bitOperations() {
    TestUtils.optimizeAssertSameVariables(
        "b=111&4 c=111|20 d=!111 e=111^4 " //
            + "println b println c println d println e",
        optimizer);
  }

  @Test
  public void boolOperations() {
    TestUtils.optimizeAssertSameVariables(
        "b=true and true c=true or false d=not false e=true xor false " //
            + "println b println c println d println e",
        optimizer);
  }
}
