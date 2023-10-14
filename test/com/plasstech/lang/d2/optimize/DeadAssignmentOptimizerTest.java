package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.testing.TestUtils;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.VarType;

public class DeadAssignmentOptimizerTest {

  private Optimizer optimizer =
      new ILOptimizer(ImmutableList.of(new DeadAssignmentOptimizer(2), new NopOptimizer()));

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

  @Test
  public void deadTempsLowLevel() {
    ProcSymbol procSym =
        new ProcSymbol(new ProcedureNode("f", ImmutableList.of(), VarType.VOID, null, null), null);
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(
                new TempLocation("temp", VarType.STRING), ConstantOperand.EMPTY_STRING, null),
            new Call(procSym, ImmutableList.of(), ImmutableList.of(), null),
            new Stop());
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(2);
  }

  @Test
  public void notDeadFunctionCall_bug249() {
    TestUtils.optimizeAssertSameVariables(
        "      pr:proc:int {\n"
            + "     print 'hi'\n"
            + "     return 3\n"
            + "    }\n"
            + "f:proc {\n"
            + "  x=pr()\n"
            + "}\n"
            + "f()",
        optimizer);
  }

  @Test
  public void notDeadFunctionCall_afterAssignment_bug249() {
    TestUtils.optimizeAssertSameVariables(
        "      pr:proc:int {\n"
            + "     print 'hi'\n"
            + "     return 3\n"
            + "    }\n"
            + "f:proc {\n"
            + "  x=pr()\n"
            + "  x=3\n"
            + "}\n"
            + "f()",
        optimizer);
  }
}
