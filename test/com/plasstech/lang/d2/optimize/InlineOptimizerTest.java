package com.plasstech.lang.d2.optimize;

import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThatInterpreting;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.testing.TestCode;

public class InlineOptimizerTest {
  private static final Optimizer OPTIMIZER =
      new ILOptimizer(
          ImmutableList.of(
              new NopOptimizer(),
              new ConstantPropagationOptimizer(0),
              new DeadAssignmentOptimizer(0),
              new DeadCodeOptimizer(0),
              new DeadLabelOptimizer(0),
              new DeadProcOptimizer(0),
              new InlineOptimizer(2)),
          2);

  @Test
  public void shortVoidNoArg() {
    assertThatInterpreting("      g = 0 "
        + "shortVoidNoArg:proc() { g = 3 } " //
        + "shortVoidNoArg() "
        + "println g").withOptimizer(OPTIMIZER).hasNoCalls();
  }

  @Test
  public void shortVoidGlobal() {
    assertThatInterpreting("      g = 0"
        + "shortVoidGlobal:proc(n:int) { g = g + n } " //
        + "shortVoidGlobal(10) "
        + "println g").withOptimizer(OPTIMIZER).hasNoCalls();
  }

  @Test
  public void shortProc() {
    assertThatInterpreting("      shortProc:proc(n:int):int { return n + 1 } "
        + "println shortProc(10)").withOptimizer(OPTIMIZER).hasNoCalls();
  }

  @Test
  public void shortProcForward() {
    assertThatInterpreting("  println shortProc(10) "
        + "shortProc:proc(n:int):int { println n return n + 1 }").withOptimizer(OPTIMIZER)
        .hasNoCalls();
  }

  @Test
  @Ignore("The 'if' check in IL prevents it from being inlined")
  public void shortProcRecord() {
    assertThatInterpreting("      rt:record{i:int} "
        + "shortProcRecord:proc():rt { "
        + "  x = new rt "
        + "  x.i=3 "
        + "  return x"
        + "} " //
        + "r = shortProcRecord() "
        + "println r.i").withOptimizer(OPTIMIZER).hasNoCalls();
  }

  @Test
  public void shortProcGlobalRecord() {
    // With the inline NPE checks, this proc is no longer small enough.
    assertThatInterpreting("      rt:record{i:int} r:rt "
        + "shortProcGlobalRecord:proc() { "
        + "  r = new rt "
        + "  r.i=3 "
        + "} " //
        + "shortProcGlobalRecord() "
        + "println r.i").withOptimizer(OPTIMIZER).hasCallsTo("shortProcGlobalRecord");
  }

  @Test
  public void shortProcWithCall() {
    assertThatInterpreting("      p:proc(n:int):int { return n+1 }"
        + "shortProcWithCall:proc(n:int):int { return p(n) } " //
        + "println shortProcWithCall(10)").withOptimizer(OPTIMIZER).hasNoCalls();
  }

  @Test
  public void medium() {
    assertThatInterpreting("      medium:proc(c:string):bool { return c >= '1' } " //
        + "println medium('12') "
        + "println medium('0')").hasNoCalls();
  }

  @Test
  public void linkedList() {
    assertThatInterpreting(TestCode.LINKED_LIST).withOptimizer(OPTIMIZER).hasSameVariables();
  }

  @Test
  public void recordLoopInvariant() {
    assertThatInterpreting(TestCode.RECORD_LOOP_INVARIANT).withOptimizer(OPTIMIZER)
        .hasSameVariables();
  }

  @Test
  public void ignoreReturnValue() {
    assertThatInterpreting("      ignoredReturnValue:proc():int {" //
        + "  return 6 "
        + "} "
        + "ignoredReturnValue()").withOptimizer(OPTIMIZER).hasNoCalls();
  }

  @Test
  public void ignoreReturnValueSometimes() {
    assertThatInterpreting("      ignoredReturnValueSometimes:proc():int {" //
        + "  return 6 "
        + "} "
        + "ignoredReturnValueSometimes() "
        + "println ignoredReturnValueSometimes()").withOptimizer(OPTIMIZER).hasNoCalls();
  }

  @Test
  public void ignoreReturnValueSometimesAllOpts() {
    assertThatInterpreting("      ignoredReturnValueSometimesAllOpts:proc():int {" //
        + "  return 6 "
        + "} "
        + "ignoredReturnValueSometimesAllOpts() "
        + "println ignoredReturnValueSometimesAllOpts()").hasNoCalls();
  }

  @Test
  public void multipleCalls() {
    assertThatInterpreting("      multipleCalls:proc(c:string):bool { return c >= '0' } " //
        + "" //
        + "println multipleCalls('12') " //
        + "println multipleCalls('3') " //
        + "println multipleCalls('no') ").withOptimizer(OPTIMIZER).hasNoCalls();
  }

  @Test
  public void twoReturns() {
    assertThatInterpreting("      twoReturns: proc(n:int):bool {"
        + "  if n>0 {return true} else {return false} "
        + "} "
        + "println twoReturns(10) "
        + "println twoReturns(-10) ").withOptimizer(OPTIMIZER).hasCallsTo("twoReturns");
  }

  @Test
  public void longProc() {
    assertThatInterpreting("      longProc: proc(n:int):int {"
        + "  sum = 0 i=0 while i < n do i = i + 1 {"
        + "    sum = sum + i"
        + "  }"
        + "  return sum"
        + "}"
        + "println longProc(10)").withOptimizer(OPTIMIZER).hasCallsTo("longProc");
  }
}
