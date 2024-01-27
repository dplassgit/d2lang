package com.plasstech.lang.d2.codegen.t100.integration;

import static org.junit.Assume.assumeTrue;

import org.junit.Before;
import org.junit.Test;

import com.plasstech.lang.d2.codegen.t100.testing.TestUtils;

public class LogicTest {
  @Before
  public void setUp() {
    // NOTE: this test will not pass in bazel
    assumeTrue(System.getenv("TEST_SRCDIR") == null);
  }

  @Test
  public void factInt() throws Exception {
    String source =
        "      n=12 // 12! = only 479001600 but 13! is too big for 31 bits\n"
            + "fact=1\n"
            + "\n"
            + "i=1 while i <= n do i++{\n"
            + "  fact = fact * i\n"
            + "  print i print '! = ' println fact\n"
            + "}";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void fibIter() throws Exception {
    String source =
        "      max = 10\n"
            + "iterative_fib:proc(n: int): int {\n"
            + "  n1 = 0\n"
            + "  n2 = 1\n"
            + "  nth = 0\n"
            + "  i=1 while i <= n do i++ {\n"
            + "    nth = n1 + n2\n"
            + "    n1 = n2\n"
            + "    n2 = nth\n"
            + "    print i \n"
            + "    print 'th fib: '\n"
            + "    println nth\n"
            + "  }\n"
            + "  return nth\n"
            + "}\n"
            + "iterative = iterative_fib(max)\n"
            + "print 'iterative=' println iterative\n";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }

  @Test
  public void manualMult() throws Exception {
    String source =
        "      mult: proc(left: int, right: int): int {\n"
            + "  answer = 0\n"
            + "  i = 0y00 while i < 0y20 do i++ {\n"
            + "    if (right & 1) == 1 {\n"
            + "      answer += left\n"
            + "    }\n"
            + "    left = left << 1 // shift left\n"
            + "    right = right >> 1  // shift right\n"
            + "  }\n"
            + "  return answer\n"
            + "}\n"
            + "\n"
            + "println mult(34567, -12345)\n"
            + "println mult(-34567, 12345)\n"
            + "println mult(34567, 12345)\n"
            + "println mult(-34567, -12345)\n";
    TestUtils.assertInterpretedEqualsEmulated(source);
  }
}
