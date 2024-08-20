package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.OpcodeSubject.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThatInterpreting;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DeallocateTemp;
import com.plasstech.lang.d2.codegen.il.Goto;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.ProcEntry;
import com.plasstech.lang.d2.codegen.il.ProcExit;
import com.plasstech.lang.d2.codegen.il.Stop;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.testing.TestCode;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.VarType;

public class DeadAssignmentOptimizerTest {
  private static Location LONG_TEMP = LocationUtils.newLongTempLocation("longtemp", VarType.INT);
  private static Location GLOBAL = LocationUtils.newMemoryAddress("global", VarType.INT);

  private Optimizer optimizer =
      new ILOptimizer(ImmutableList.of(new NopOptimizer(), new DeadAssignmentOptimizer(2)), 0);

  @Test
  public void notDeadParams() {
    assertThatInterpreting("      p:proc(n:int):int {" //
        + "  n=n*2 x=n-1 return x+n " //
        + "}" //
        + "println p(10)").withOptimizer(optimizer).hasSameVariables();
  }

  @Test
  public void notDeadArraySetGlobal() {
    assertThatInterpreting("a:int[2] d=1 a[d]=d println a[d]").withOptimizer(optimizer)
        .hasSameVariables();
  }

  @Test
  public void notDeadArraySetLocal() {
    InterpreterResult result =
        assertThatInterpreting("p:proc() {a:int[2] d=1 a[d]=d print a[d]} p()")
            .withOptimizer(optimizer).hasSameVariables();
    assertThat(result.environment().output()).containsExactly("1");
  }

  @Test
  public void deadTemps() {
    assertThatInterpreting("      p:proc(n:int):int {"
        + "  sum = 0 i=0 while i < n do i = i + 1 {"
        + "    y = n * (2-1)"
        + "    y = n * (n-1) + n"
        + "    sum = sum + i"
        + "  }"
        + "  return sum"
        + "}"
        + "println p(10)").withOptimizer(optimizer).hasSameVariables();
  }

  @Test
  public void recordLoopInvariant() {
    assertThatInterpreting("      rt: record{i:int} "
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
        + "println val").withOptimizer(optimizer).hasSameVariables();
  }

  @Test
  public void linkedList() {
    assertThatInterpreting(TestCode.LINKED_LIST).withOptimizer(optimizer).hasSameVariables();
  }

  @Test
  public void deadTempsLowLevel() {
    ProcSymbol procSym =
        new ProcSymbol(new ProcedureNode("f", ImmutableList.of(), VarType.VOID, null, null), null);
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(
                LocationUtils.newTempLocation("temp", VarType.STRING), ConstantOperand.EMPTY_STRING,
                null),
            new Call(procSym, ImmutableList.of(), ImmutableList.of(), null),
            new Stop());
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(2);
    assertThat(optimized.get(0)).isInstanceOf(Call.class);
    assertThat(optimized.get(1)).isInstanceOf(Stop.class);
  }

  @Test
  public void notDeadFunctionCall_bug249() {
    assertThatInterpreting("      pr:proc:int {\n"
        + "     print 'hi'\n"
        + "     return 3\n"
        + "    }\n"
        + "f:proc {\n"
        + "  x=pr()\n"
        + "}\n"
        + "f()").withOptimizer(optimizer).hasSameVariables();
  }

  @Test
  public void notDeadFunctionCall_afterAssignment_bug249() {
    assertThatInterpreting("      pr:proc:int {\n"
        + "     print 'hi'\n"
        + "     return 3\n"
        + "    }\n"
        + "f:proc {\n"
        + "  x=pr()\n"
        + "  x=3\n"
        + "}\n"
        + "f()").withOptimizer(optimizer).hasSameVariables();
  }

  @Test
  public void notDeadFunctionCall_beforeAssignment_bug249() {
    assertThatInterpreting("      pr:proc:int {\n"
        + "     print 'hi'\n"
        + "     return 3\n"
        + "    }\n"
        + "f:proc {\n"
        + "  x=3\n"
        + "  x=pr()\n"
        + "}\n"
        + "f()").withOptimizer(optimizer).hasSameVariables();
  }

  @Test
  public void nonDeadLongTemp() {
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(LONG_TEMP, ConstantOperand.ZERO, null),
            new Transfer(LocationUtils.newParamLocation("dest", VarType.INT, 0, 0), LONG_TEMP,
                null),
            new DeallocateTemp(LONG_TEMP, null));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(3);
  }

  @Test
  public void deadLongTempNoDeallocate() {
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(LONG_TEMP, ConstantOperand.ZERO, null),
            new Transfer(LocationUtils.newParamLocation("dest", VarType.INT, 0, 0), LONG_TEMP,
                null));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(2);
  }

  @Test
  public void deadLongTemp() {
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(LONG_TEMP, ConstantOperand.ZERO, null),
            new DeallocateTemp(LONG_TEMP, null));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(0);
  }

  @Test
  public void deadGlobalTransfer() {
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(GLOBAL, ConstantOperand.ZERO, null), // dead
            new Transfer(GLOBAL, ConstantOperand.ONE, null));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.ONE);
  }

  @Test
  public void deadGlobalBinOp() {
    ImmutableList<Op> code =
        ImmutableList.of(
            new BinOp(GLOBAL, ConstantOperand.ZERO, TokenType.PLUS, ConstantOperand.ZERO, null), // dead
            new Transfer(GLOBAL, ConstantOperand.ONE, null));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.ONE);
  }

  @Test
  public void deadGlobalUnaryOp() {
    ImmutableList<Op> code =
        ImmutableList.of(
            new UnaryOp(GLOBAL, TokenType.PLUS, ConstantOperand.ZERO, null), // dead
            new Transfer(GLOBAL, ConstantOperand.ONE, null));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.ONE);
  }

  @Test
  public void nonDeadGlobalProcStart() {
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(GLOBAL, ConstantOperand.ONE, null),
            new ProcEntry("name", ImmutableList.of(), 0));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).isEqualTo(code);
  }

  @Test
  public void nonDeadGlobalGoto() {
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(GLOBAL, ConstantOperand.ONE, null),
            new Goto("label"));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).isEqualTo(code);
  }

  @Test
  public void notDeadGlobalInProc() {
    assertThatInterpreting("      g = 0 "
        + "g = 1 "
        // this 'g' should not be dead.
        + "shortVoidGlobal:proc(n:int) { g = g + n } "
        + "shortVoidGlobal(10) "
        + "println g").withOptimizer(optimizer).hasSameVariables();
  }

  private static final Location B = LocationUtils.newParamLocation("b", null, 0, 0);
  private static final Location C = LocationUtils.newParamLocation("c", null, 0, 0);

  @Test
  public void longTempDestinationOnly_nops() {
    ImmutableList<Op> code = ImmutableList.of(new BinOp(LONG_TEMP, B, TokenType.PLUS, C, null),
        new ProcExit(null, 0, 0));
    ImmutableList<Op> output = optimizer.optimize(code, null);
    assertThat(output).hasSize(1);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(output.get(0)).isInstanceOf(ProcExit.class);
  }
}
