package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.OpcodeVisitor;
import com.plasstech.lang.d2.interpreter.ExecutionResult;
import com.plasstech.lang.d2.testing.TestUtils;

public class InlineOptimizerTest {
  private Optimizer optimizer = new ILOptimizer(new InlineOptimizer(2)).setDebugLevel(2);

  @Test
  public void shortVoidNoArg() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      g = 0 "
                + "shortVoidNoArg:proc() { g = 3 } " //
                + "shortVoidNoArg() "
                + "println g",
            optimizer);

    ImmutableList<Op> code = result.code();
    assertNoCalls(code);
  }

  @Test
  public void shortVoidLocal() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      shortVoidLocal:proc(n:int) { m = n + 1 print m } " //
                + "shortVoidLocal(3) ",
            optimizer);

    ImmutableList<Op> code = result.code();
    assertNoCalls(code);
  }

  @Test
  public void shortVoidGlobal() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      g = 0"
                + "shortVoidGlobal:proc(n:int) { g = g + n } " //
                + "shortVoidGlobal(10) "
                + "println g",
            optimizer);

    ImmutableList<Op> code = result.code();
    assertNoCalls(code);
  }

  @Test
  public void shortProc() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      shortProc:proc(n:int):int { return n + 1 } " //
                + "println shortProc(10)",
            optimizer);

    ImmutableList<Op> code = result.code();
    assertNoCalls(code);
  }

  @Test
  public void shortProcRecord() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      rt:record{i:int} "
                + "shortProcRecord:proc():rt { "
                + "  x = new rt "
                + "  x.i=3 "
                + "  return x"
                + "} " //
                + "r = shortProcRecord() "
                + "println r.i",
            optimizer);

    ImmutableList<Op> code = result.code();
    // show that there are no calls to the procedure
    assertNoCalls(code);
  }

  @Test
  public void shortProcGlobalRecord() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      rt:record{i:int} r:rt "
                + "shortProcGlobalRecord:proc() { "
                + "  r = new rt "
                + "  r.i=3 "
                + "} " //
                + "shortProcGlobalRecord() "
                + "println r.i",
            optimizer);

    ImmutableList<Op> code = result.code();
    // show that there are no calls to the procedure
    assertNoCalls(code);
  }

  @Test
  public void shortProcWithCall() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      p:proc(n:int):int { return n+1 }"
                + "shortProcWithCall:proc(n:int):int { return p(n) } " //
                + "println shortProcWithCall(10)",
            optimizer);

    ImmutableList<Op> code = result.code();
    assertNoCalls(code);
  }

  @Test
  public void medium() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      medium:proc(c:string):bool { return c >= '0' and c <= '9' } " //
                + "println medium('12')",
            optimizer);

    ImmutableList<Op> code = result.code();
    assertNoCalls(code);
  }

  @Test
  public void linkedList() {
    TestUtils.optimizeAssertSameVariables(TestUtils.LINKED_LIST, optimizer);
  }

  @Test
  public void recordLoopInvariant() {
    TestUtils.optimizeAssertSameVariables(TestUtils.RECORD_LOOP_INVARIANT, optimizer);
  }

  @Test
  public void ignoreReturnValue() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      ignoredReturnValue:proc():int {" //
                + "  return 6 "
                + "} "
                + "ignoredReturnValue()",
            optimizer);
    ImmutableList<Op> code = result.code();
    assertNoCalls(code);
  }

  @Test
  public void ignoreReturnValueSometimes() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      ignoredReturnValueSometimes:proc():int {" //
                + "  return 6 "
                + "} "
                + "ignoredReturnValueSometimes() "
                + "println ignoredReturnValueSometimes()",
            optimizer);
    ImmutableList<Op> code = result.code();
    assertNoCalls(code);
  }

  @Test
  public void ignoreReturnValueSometimesAllOpts() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      ignoredReturnValueSometimesAllOpts:proc():int {" //
                + "  return 6 "
                + "} "
                + "ignoredReturnValueSometimesAllOpts() "
                + "println ignoredReturnValueSometimesAllOpts()");
    ImmutableList<Op> code = result.code();
    assertNoCalls(code);
  }

  @Test
  public void multipleCalls() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      multipleCalls:proc(c:string):bool { return c >= '0' and c <= '9' } " //
                + "" //
                + "println multipleCalls('12') " //
                + "println multipleCalls('3') " //
                + "println multipleCalls('no') ",
            optimizer);

    ImmutableList<Op> code = result.code();
    assertNoCalls(code);
  }

  @Test
  public void longProc() {
    ExecutionResult result =
        TestUtils.optimizeAssertSameVariables(
            "      longProc: proc(n:int):int {"
                + "  sum = 0 i=0 while i < n do i = i + 1 {"
                + "    sum = sum + i"
                + "  }"
                + "  return sum"
                + "}"
                + "println longProc(10)",
            optimizer);
    ImmutableList<Op> code = result.code();

    // Show that there are still calls to the procedure
    OpcodeVisitor visitor =
        new DefaultOpcodeVisitor() {
          @Override
          public void visit(Call op) {
            assertThat(op.functionToCall()).isEqualTo("longProc");
          }
        };
    for (Op op : code) {
      op.accept(visitor);
    }
  }

  private static final OpcodeVisitor NO_CALLS =
      new DefaultOpcodeVisitor() {
        @Override
        public void visit(Call op) {
          fail("Should not call any procs");
        }
      };

  private static void assertNoCalls(ImmutableList<Op> code) {
    for (Op op : code) {
      op.accept(NO_CALLS);
    }
  }
}
