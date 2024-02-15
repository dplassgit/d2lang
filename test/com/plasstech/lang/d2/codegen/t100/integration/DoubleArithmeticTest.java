package com.plasstech.lang.d2.codegen.t100.integration;

import static org.junit.Assume.assumeTrue;

import org.junit.Before;
import org.junit.Test;

import com.plasstech.lang.d2.codegen.t100.testing.TestUtils;

public class DoubleArithmeticTest {
  @Before
  public void setUp() {
    // NOTE: this test will not pass in bazel
    assumeTrue(System.getenv("TEST_SRCDIR") == null);
  }

  @Test
  public void assign() throws Exception {
    String source = "left=1.0";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void transfer() throws Exception {
    String source = "left=1.0 right=left";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void add() throws Exception {
    String source = "left=1.0 right=2.0\n"
        + "sum:proc(a:double, b:double): double {\n"
        + " return a + b\n"
        + "}\n"
        + "println sum(left, right)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }
}
