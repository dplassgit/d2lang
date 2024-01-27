package com.plasstech.lang.d2.codegen.t100.integration;

import static org.junit.Assume.assumeTrue;

import org.junit.Before;
import org.junit.Test;

import com.plasstech.lang.d2.codegen.t100.testing.TestUtils;

public class ComparisonTest {
  @Before
  public void setUp() {
    // NOTE: this test will not pass in bazel
    assumeTrue(System.getenv("TEST_SRCDIR") == null);
  }

  @Test
  public void eq_int() throws Exception {
    String source =
        "eq: proc(a:int, b:int) { println a==b } \n"
            + "eq(1, 1)\n"
            + "eq(1, 2)\n"
            + "eq(2, 1)\n"
            + "eq(-2, 2)\n"
            + "eq(2, -2)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void neq_int() throws Exception {
    String source =
        "neq: proc(a:int, b:int) { println a!=b } \n"
            + "neq(1, 1)\n"
            + "neq(1, 2)\n"
            + "neq(2, 1)\n"
            + "neq(-2, 2)\n"
            + "neq(2, -2)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void eq_byte() throws Exception {
    String source =
        "eq: proc(a:byte, b:byte) { println a==b } \n"
            + "eq(0y01, 0y01)\n"
            + "eq(0y01, 0y02)\n"
            + "eq(0y02, 0y01)\n"
            + "eq(-0y02, 0y02)\n"
            + "eq(0y02, -0y02)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void neq_byte() throws Exception {
    String source =
        "neq: proc(a:byte, b:byte) { println a!=b } \n"
            + "neq(0y01, 0y01)\n"
            + "neq(0y01, 0y02)\n"
            + "neq(0y02, 0y01)\n"
            + "neq(-0y02, 0y02)\n"
            + "neq(0y02, -0y02)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void gt_int() throws Exception {
    String source =
        "gt: proc(a:int, b:int) { println a>b } \n"
            + "gt(1, 1)\n"
            + "gt(1, 2)\n"
            + "gt(2, 1)\n"
            + "gt(-2, 2)\n"
            + "gt(2, -2)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void ge_int() throws Exception {
    String source =
        "ge: proc(a:int, b:int) { println a>=b } \n"
            + "ge(1, 1)\n"
            + "ge(1, 2)\n"
            + "ge(2, 1)\n"
            + "ge(-2, 2)\n"
            + "ge(2, -2)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void gt_byte() throws Exception {
    String source =
        "gt: proc(a:byte, b:byte) { println a>b } \n"
            + "gt(0y01, 0y01)\n"
            + "gt(0y01, 0y02)\n"
            + "gt(0y02, 0y01)\n"
            + "gt(-0y02, 0y02)\n"
            + "gt(0y02, -0y02)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void ge_byte() throws Exception {
    String source =
        "ge: proc(a:byte, b:byte) { println a>=b } \n"
            + "ge(0y01, 0y01)\n"
            + "ge(0y01, 0y02)\n"
            + "ge(0y02, 0y01)\n"
            + "ge(-0y02, 0y02)\n"
            + "ge(0y02, -0y02)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void lt_int() throws Exception {
    String source =
        "lt: proc(a:int, b:int) { println a<b } \n"
            + "lt(1, 1)\n"
            + "lt(1, 2)\n"
            + "lt(2, 1)\n"
            + "lt(-2, 2)\n"
            + "lt(2, -2)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void le_int() throws Exception {
    String source =
        "le: proc(a:int, b:int) { println a<=b } \n"
            + "le(1, 1)\n"
            + "le(1, 2)\n"
            + "le(2, 1)\n"
            + "le(-2, 2)\n"
            + "le(2, -2)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void lt_byte() throws Exception {
    String source =
        "lt: proc(a:byte, b:byte) { println a<b } \n"
            + "lt(0y01, 0y01)\n"
            + "lt(0y01, 0y02)\n"
            + "lt(0y02, 0y01)\n"
            + "lt(-0y02, 0y02)\n"
            + "lt(0y02, -0y02)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void le_byte() throws Exception {
    String source =
        "le: proc(a:byte, b:byte) { println a<=b } \n"
            + "le(0y01, 0y01)\n"
            + "le(0y01, 0y02)\n"
            + "le(0y02, 0y01)\n"
            + "le(-0y02, 0y02)\n"
            + "le(0y02, -0y02)";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }
}