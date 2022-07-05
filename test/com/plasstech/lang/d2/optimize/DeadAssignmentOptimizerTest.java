package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.testing.TestUtils;

public class DeadAssignmentOptimizerTest {

  private Optimizer optimizer = new ILOptimizer(new DeadAssignmentOptimizer(2)).setDebugLevel(2);

  @Test
  public void notDeadParams() {
    TestUtils.optimizeAssertSameVariables(
        "      p:proc(n:int):int {" //
            + "  n=n*2 x=n-1 return x+n " //
            + "}" //
            + "println p(10)",
        optimizer);
  }

  @Test
  public void notDeadArraySetGlobal() {
    TestUtils.optimizeAssertSameVariables("a:int[2] d=1 a[d]=d println a[d]", optimizer);
  }

  @Test
  public void notDeadArraySetLocal() {
    InterpreterResult result =
        TestUtils.optimizeAssertSameVariables(
            "p:proc() {a:int[2] d=1 a[d]=d print a[d]} p()", optimizer);
    assertThat(result.environment().output()).containsExactly("1");
  }

  @Test
  public void deadTemps() {
    TestUtils.optimizeAssertSameVariables(
        "      p:proc(n:int):int {"
            + "  sum = 0 i=0 while i < n do i = i + 1 {"
            + "    y = n * (2-1)"
            + "    y = n * (n-1) + n"
            + "    sum = sum + i"
            + "  }"
            + "  return sum"
            + "}"
            + "println p(10)",
        optimizer);
  }

  @Test
  public void recordLoopInvariant() {
    TestUtils.optimizeAssertSameVariables(
        "      rt: record{i:int} "
            + "updaterec: proc(re:rt) { "
            + "  re.i = re.i + 1 "
            + "} "
            + "recordloopinvariant: proc(rec:rt): int { "
            + "  rec.i = 0"
            + "  while rec.i < 10 { "
            + "    updaterec(rec) "
            + "  } "
            + "  return rec.i "
            + "} "
            + "val = recordloopinvariant(new rt) "
            + "println val",
        optimizer);
  }

  @Test
  public void linkedList() {
    TestUtils.optimizeAssertSameVariables(TestUtils.LINKED_LIST, optimizer);
  }
}
