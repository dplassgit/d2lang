package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.codegen.il.testing.OpcodeSubject.assertThat;
import static com.plasstech.lang.d2.interpreter.testing.InterpreterSubject.assertThatInterpreting;
import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThat;

import java.util.List;

import org.junit.Ignore;
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
import com.plasstech.lang.d2.optimize.testing.OptimizerWithNop;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.testing.TestCode;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.VarType;

public class DeadAssignmentOptimizerTest {
  private final Optimizer optimizer = new OptimizerWithNop(new DeadAssignmentOptimizer(2));

  private static final Location LONG_TEMP =
      LocationUtils.newLongTempLocation("longtemp", VarType.INT);
  private static final Location GLOBAL = LocationUtils.newMemoryAddress("global", VarType.INT);
  private static final Location B = LocationUtils.newParamLocation("b", VarType.INT, 0, 0);
  private static final Location C = LocationUtils.newParamLocation("c", VarType.INT, 0, 0);
  private static final ProcSymbol PROC_SYMBOL =
      new ProcSymbol(new ProcedureNode("f", ImmutableList.of(), VarType.VOID, null, null), null);

  @Test
  public void notDeadParams() {
    assertThatInterpreting(
            """
            p:proc(n:int):int {
              n=n*2
              x=n-1
              return x+n
            }
            println p(10)
            """)
        .withOptimizer(optimizer)
        .hasSameVariables();
  }

  @Test
  public void notDeadArraySetGlobal() {
    assertThatInterpreting("a:int[2] d=1 a[d]=d println a[d]")
        .withOptimizer(optimizer)
        .hasSameVariables();
  }

  @Test
  public void notDeadArraySetLocal() {
    InterpreterResult result =
        assertThatInterpreting("p:proc() {a:int[2] d=1 a[d]=d print a[d]} p()")
            .withOptimizer(optimizer)
            .hasSameVariables();
    assertThat(result.environment().output()).containsExactly("1");
  }

  @Test
  public void deadTemps() {
    assertThatInterpreting(
            """
            p:proc(n:int):int {
              sum = 0 i=0 while i < n do i = i + 1 {
                y = n * (2-1)
                y = n * (n-1) + n
                sum = sum + i
              }
              return sum
            }
            println p(10)
            """)
        .withOptimizer(optimizer)
        .hasSameVariables();
  }

  @Test
  public void recordLoopInvariant() {
    assertThatInterpreting(
            """
            rt: record{i:int}
            updaterec: proc(re:rt) {
              re.i = re.i + 1
            }
            recordloopinvariant: proc(rec:rt): int {
              rec.i = 0
              while rec.i < 10 {
                updaterec(rec)
              }
              return rec.i
            }
            val = recordloopinvariant(new rt)
            println val
            """)
        .withOptimizer(optimizer)
        .hasSameVariables();
  }

  @Test
  public void linkedList() {
    assertThatInterpreting(TestCode.LINKED_LIST).withOptimizer(optimizer).hasSameVariables();
  }

  @Test
  public void deadTempsLowLevel() {
    // temp = "" // dead
    // f();
    // exit
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(
                LocationUtils.newTempLocation("temp", VarType.STRING),
                ConstantOperand.EMPTY_STRING,
                null),
            new Call(PROC_SYMBOL, ImmutableList.of(), ImmutableList.of(), null),
            new Stop());
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(2);
    assertThat(optimized.get(0)).isInstanceOf(Call.class);
    assertThat(optimized.get(1)).isInstanceOf(Stop.class);
  }

  @Test
  public void notDeadFunctionCall_bug249() {
    assertThatInterpreting(
            """
            pr:proc:int {
              print 'hi'
              return 3
            }
            f:proc {
              x=pr()
            }
            f()
            """)
        .withOptimizer(optimizer)
        .hasSameVariables();
  }

  @Test
  public void notDeadFunctionCall_afterAssignment_bug249() {
    assertThatInterpreting(
            """
            pr:proc:int {
              print 'hi'
              return 3
            }
            f:proc {
              x=pr()
              x=3
            }
            f()
            """)
        .withOptimizer(optimizer)
        .hasSameVariables();
  }

  @Test
  public void notDeadFunctionCall_beforeAssignment_bug249() {
    assertThatInterpreting(
            """
            pr:proc:int {
              print 'hi'
              return 3
            }
            f:proc {
              x=3
              x=pr()
            }
            f()
            """)
        .withOptimizer(optimizer)
        .hasSameVariables();
  }

  @Test
  public void nonDeadLongTemp() {
    // long_temp = 0    // not dead
    // dest = long_temp // dead
    // deallocate long temp
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(LONG_TEMP, ConstantOperand.ZERO, null),
            new Transfer(
                LocationUtils.newParamLocation("dest", VarType.INT, 0, 0), LONG_TEMP, null),
            new DeallocateTemp(LONG_TEMP, null));
    List<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimizer).isChanged();
    assertThat(optimized.get(0)).isEqualTo(code.get(0));
  }

  @Test
  public void nonDeadLongTempNoDeallocate() {
    // long_temp = 0    // not dead
    // dest = long_temp // dead
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(LONG_TEMP, ConstantOperand.ZERO, null),
            new Transfer(
                LocationUtils.newParamLocation("dest", VarType.INT, 0, 0), LONG_TEMP, null));
    List<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(1);
    assertThat(optimizer).isChanged();
    assertThat(optimized.get(0)).isEqualTo(code.get(0));
  }

  @Test
  public void deadLongTemp() {
    // long_temp = 0 // dead
    // deallocate long temp
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(LONG_TEMP, ConstantOperand.ZERO, null),
            new DeallocateTemp(LONG_TEMP, null));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(0);
  }

  @Test
  public void deadGlobalTransfer() {
    // global = 0 // dead
    // global = 1
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(GLOBAL, ConstantOperand.ZERO, null), // dead
            new Transfer(GLOBAL, ConstantOperand.ONE, null));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(0);
  }

  @Test
  public void deadGlobalBinOp() {
    // global = 0 + 0 // dead
    // global = 1
    ImmutableList<Op> code =
        ImmutableList.of(
            new BinOp(
                GLOBAL, ConstantOperand.ZERO, TokenType.PLUS, ConstantOperand.ZERO, null), // dead
            new Transfer(GLOBAL, ConstantOperand.ONE, null));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(0);
  }

  @Test
  public void deadGlobalUnaryOp() {
    // global = +0 // dead
    // global = 1 // also dead (!)
    ImmutableList<Op> code =
        ImmutableList.of(
            new UnaryOp(GLOBAL, TokenType.PLUS, ConstantOperand.ZERO, null), // dead
            new Transfer(GLOBAL, ConstantOperand.ONE, null));
    ImmutableList<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimized).hasSize(0);
  }

  @Test
  public void nonDeadGlobalProcStart() {
    // global = 1 // not dead
    // proc_entry
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(GLOBAL, ConstantOperand.ONE, null),
            new ProcEntry("name", ImmutableList.of(), 0));
    optimizer.optimize(code, null);
    assertThat(optimizer).isNotChanged();
  }

  @Test
  public void nonDeadGlobalGoto() {
    // global = 1 // not dead
    // goto label
    ImmutableList<Op> code =
        ImmutableList.of(new Transfer(GLOBAL, ConstantOperand.ONE, null), new Goto("label"));
    optimizer.optimize(code, null);
    assertThat(optimizer).isNotChanged();
  }

  @Test
  public void nonDeadGlobalCall() {
    // global = 1 // not dead
    // f()
    // global = 0 // dead
    ImmutableList<Op> code =
        ImmutableList.of(
            new Transfer(GLOBAL, ConstantOperand.ONE, null),
            new Call(PROC_SYMBOL, ImmutableList.of(), ImmutableList.of(), null),
            new Transfer(GLOBAL, ConstantOperand.ZERO, null));
    List<Op> optimized = optimizer.optimize(code, null);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(2);
    assertThat(optimized.get(0)).isEqualTo(code.get(0));
    assertThat(optimized.get(1)).isEqualTo(code.get(1));
  }

  @Test
  public void notDeadGlobalInProc() {
    assertThatInterpreting(
            """
            g = 0
            // this 'g' should not be dead.
            g = 1
            shortVoidGlobal:proc(n:int) {
              g = g + n
            }
            shortVoidGlobal(10)
            println g
            """)
        .withOptimizer(optimizer)
        .hasSameVariables();
    assertThat(optimizer).isChanged();
  }

  @Test
  @Ignore
  // Not sure what this is testing; it always removes the long temp. I don't know if it's right or
  // wrong, though.
  public void longTempDestinationOnly_nops() {
    // long_temp = b + c // not dead, since there is no deallocation
    // proc exit
    ImmutableList<Op> code =
        ImmutableList.of(
            new BinOp(LONG_TEMP, B, TokenType.PLUS, C, null), new ProcExit(null, 0, 0));
    optimizer.optimize(code, null);
    assertThat(optimizer).isNotChanged();
  }
}
