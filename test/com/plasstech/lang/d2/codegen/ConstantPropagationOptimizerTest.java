package com.plasstech.lang.d2.codegen;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class ConstantPropagationOptimizerTest {
  private ConstantPropagationOptimizer optimizer = new ConstantPropagationOptimizer(2);

  @Test
  public void simple() {
    TestUtils.optimizeAssertSameVariables("p:proc() {a=3 println a+1} p()", optimizer);
  }

  @Test
  public void indirect() {
    TestUtils.optimizeAssertSameVariables("p:proc() {a=3 b=a+1 c=b*2 print a print b print c} p()", optimizer);
  }
}
