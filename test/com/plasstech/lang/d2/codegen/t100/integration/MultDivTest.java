package com.plasstech.lang.d2.codegen.t100.integration;

import static org.junit.Assume.assumeTrue;

import org.junit.Before;
import org.junit.Test;

import com.plasstech.lang.d2.codegen.t100.testing.TestUtils;

public class MultDivTest {
  @Before
  public void setUp() {
    // NOTE: this test will not pass in bazel
    assumeTrue(System.getenv("TEST_SRCDIR") == null);
  }

  @Test
  public void multInt() throws Exception {
    String source =
        "      left=504 right=-1711\n"
            + "mult: proc(left: int, right: int): int { return left * right }\n"
            + "println mult(right, left)\n"
            + "println mult(left, right)\n"
            + "println mult(left, left)\n"
            + "println mult(right, right)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void divInt() throws Exception {
    String source =
        "      left=-1234560 right=2340\n"
            + "div: proc(left: int, right: int): int { return left / right }\n"
            + "println div(left, right)\n"
            + "println div(-left, -right)\n"
            + "println div(left, -right)\n"
            + "println div(left, left)\n"
            + "println div(right, right)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }
}
