package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.codegen.il.testing.OpcodeSubject.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.MemoryAddress;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.Dec;
import com.plasstech.lang.d2.codegen.il.Inc;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.optimize.testing.OptimizerWithNop;
import com.plasstech.lang.d2.parse.node.BlockNode;
import com.plasstech.lang.d2.parse.node.ProcedureNode;
import com.plasstech.lang.d2.type.ProcSymbol;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.testing.IntegralTypeProvider;

@RunWith(TestParameterInjector.class)
public class ConstantPropagationOptimizerTest {
  private final Optimizer optimizer = new OptimizerWithNop(new ConstantPropagationOptimizer(2));

  private static final TempLocation TEMP_INT1 =
      LocationUtils.newTempLocation("__temp1", VarType.INT);
  private static final TempLocation TEMP_INT2 =
      LocationUtils.newTempLocation("__temp2", VarType.INT);
  private static final StackLocation STACK_INT1 =
      LocationUtils.newStackLocation("s1", VarType.INT, 0);
  private static final MemoryAddress GLOBAL_INT1 =
      LocationUtils.newMemoryAddress("g1", VarType.INT);
  private static final MemoryAddress GLOBAL_INT2 =
      LocationUtils.newMemoryAddress("g2", VarType.INT);

  private static final Location LONG_TEMP =
      LocationUtils.newLongTempLocation("longtemp", VarType.INT);

  @Test
  public void twoTransfers() {
    /**
     *
     *
     * <pre>
     * t1 = 1
     * t2 = t1
     * s1 = t2
     *
     * should become
     * // t1 = 1
     * t2 = 1
     * s1 = t2
     *
     * // t2 = 1
     * s1 = 1
     * </pre>
     */
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP_INT1, ConstantOperand.ONE, null),
            new Transfer(TEMP_INT2, TEMP_INT1, null),
            new Transfer(STACK_INT1, TEMP_INT2, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    // The second temp assignment won't be rmeoved by this optimizer because
    // its value will never be read.
    assertThat(optimized).hasSize(2);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.ONE);
    assertThat(optimized.get(1)).isTransferredFrom(ConstantOperand.ONE);
  }

  @Test
  public void transferToGlobal() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(GLOBAL_INT1, ConstantOperand.ONE, null),
            new Transfer(TEMP_INT1, GLOBAL_INT1, null), //
            new Transfer(GLOBAL_INT2, TEMP_INT1, null));

    program = optimizer.optimize(program, null);
    assertThat(program).hasSize(2);
    assertThat(program.get(0)).isTransferredFrom(ConstantOperand.ONE);
    assertThat(program.get(1)).isTransferredFrom(ConstantOperand.ONE);
  }

  @Test
  public void destinationOverwrittenInBinOpShouldNotPropagate() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(GLOBAL_INT1, ConstantOperand.ONE, null),
            // It's overwritten, so it should clear out the previous constant value.
            new BinOp(GLOBAL_INT1, GLOBAL_INT2, TokenType.PLUS, ConstantOperand.ONE, null),
            new Return("", GLOBAL_INT1));

    program = optimizer.optimize(program, null);
    assertThat(program).hasSize(3);
    Return returnOp = (Return) program.get(2);
    assertThat(returnOp.returnValueLocation()).hasValue(GLOBAL_INT1);
  }

  @Test
  public void destinationOverwrittenInUnaryShouldNotPropagate() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(GLOBAL_INT1, ConstantOperand.ONE, null),
            new UnaryOp(GLOBAL_INT1, TokenType.MINUS, GLOBAL_INT1, null),
            new Return("", GLOBAL_INT1));

    program = optimizer.optimize(program, null);
    assertThat(program).hasSize(3);
    Return returnOp = (Return) program.get(2);
    assertThat(returnOp.returnValueLocation()).hasValue(GLOBAL_INT1);
  }

  @Test
  public void sourceAsDestinationShouldPropagate() {
    /*
     * a=1 b=a a=2 return b // should return 1.
     */
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(GLOBAL_INT1, ConstantOperand.ONE, null),
            new Transfer(GLOBAL_INT2, GLOBAL_INT1, null),
            new Transfer(GLOBAL_INT1, ConstantOperand.of(2), null),
            new Return("", GLOBAL_INT2));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized).hasSize(program.size());
    Return returnOp = (Return) optimized.get(3);
    assertThat(returnOp.returnValueLocation()).hasValue(ConstantOperand.ONE);
  }

  @Test
  public void inc(@TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);
    Location global = LocationUtils.newMemoryAddress("global", varType);
    ImmutableList<Op> program =
        ImmutableList.of(new Transfer(global, zero, null), new Inc(global, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized).hasSize(2);

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    assertThat(optimized.get(1)).isTransferredFrom(one);
  }

  @Test
  public void dec(@TestParameter(valuesProvider = IntegralTypeProvider.class) VarType varType) {

    ConstantOperand<? extends Number> one = ConstantOperand.fromValue(1, varType);
    Location global = LocationUtils.newMemoryAddress("global", varType);
    ImmutableList<Op> program =
        ImmutableList.of(new Transfer(global, one, null), new Dec(global, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized).hasSize(2);

    ConstantOperand<? extends Number> zero = ConstantOperand.fromValue(0, varType);
    assertThat(optimized.get(1)).isTransferredFrom(zero);
  }

  @Test
  public void syscall() {
    ConstantOperand<Integer> one = ConstantOperand.of(1);
    Location param = LocationUtils.newParamLocation("param", VarType.INT, 0, 0);
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(param, one, null),
            new SysCall(
                SysCall.Call.PARAMETERIZED_MESSAGE, ImmutableList.of(ConstantOperand.ZERO, param)));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized).hasSize(2);
    SysCall newCall = (SysCall) optimized.get(1);
    assertThat(newCall.operands().get(1)).isEqualTo(one);
  }

  @Test
  public void identity() {
    ConstantOperand<Integer> four = ConstantOperand.of(4);
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(STACK_INT1, four, null), new Transfer(STACK_INT1, STACK_INT1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized).hasSize(2);
    assertThat(optimized.get(0)).isTransferredFrom(four);
    assertThat(optimized.get(1)).isTransferredFrom(four);
  }

  @Test
  public void longTemp_propagation() {
    ConstantOperand<Integer> four = ConstantOperand.of(4);
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(LONG_TEMP, four, null), new Transfer(STACK_INT1, LONG_TEMP, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized).hasSize(2);
    assertThat(optimized.get(0)).isTransferredFrom(four);
    assertThat(optimized.get(1)).isTransferredFrom(four);
  }

  @Test
  public void transfersThenReturn_bug360() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(STACK_INT1, GLOBAL_INT1, null),
            new Transfer(GLOBAL_INT1, ConstantOperand.ONE, null),
            new Return("proc", STACK_INT1));

    optimizer.optimize(program, null);
    assertThat(optimizer).isNotChanged();
  }

  @Test
  public void binOp_bug360() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(STACK_INT1, GLOBAL_INT1, null),
            new BinOp(GLOBAL_INT1, STACK_INT1, TokenType.PLUS, ConstantOperand.ONE, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized.get(1))
        .isBinOp(GLOBAL_INT1, GLOBAL_INT1, TokenType.PLUS, ConstantOperand.ONE);
  }

  @Test
  public void transfers_bug360() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(STACK_INT1, GLOBAL_INT1, null),
            new Transfer(GLOBAL_INT1, STACK_INT1, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized.get(1)).isTransferredFrom(GLOBAL_INT1);
  }

  @Test
  public void binOpThenReturn_bug360() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(STACK_INT1, GLOBAL_INT1, null),
            new BinOp(GLOBAL_INT1, STACK_INT1, TokenType.PLUS, ConstantOperand.ONE, null),
            new Return("proc", STACK_INT1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized.get(1))
        .isBinOp(GLOBAL_INT1, GLOBAL_INT1, TokenType.PLUS, ConstantOperand.ONE);
    assertThat(optimized.get(2)).isReturning(STACK_INT1);
  }

  @Test
  public void unaryOpThenReturn_bug360() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(STACK_INT1, GLOBAL_INT1, null),
            new UnaryOp(GLOBAL_INT1, TokenType.MINUS, STACK_INT1, null),
            new Return("proc", STACK_INT1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized.get(1)).isUnaryOp(GLOBAL_INT1, TokenType.MINUS, GLOBAL_INT1);
    assertThat(optimized.get(2)).isReturning(STACK_INT1);
  }

  @Test
  public void incThenReturn_bug360() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(STACK_INT1, GLOBAL_INT1, null),
            new Inc(GLOBAL_INT1, null),
            new Return("proc", STACK_INT1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized.get(1)).isInc(GLOBAL_INT1);
    assertThat(optimized.get(2)).isReturning(STACK_INT1);
  }

  @Test
  public void decThenReturn_bug360() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(STACK_INT1, GLOBAL_INT1, null),
            new Dec(GLOBAL_INT1, null),
            new Return("proc", STACK_INT1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized.get(1)).isDec(GLOBAL_INT1);
    assertThat(optimized.get(2)).isReturning(STACK_INT1);
  }

  @Test
  public void callThenReturn_bug360() {
    ProcSymbol procSymbol =
        new ProcSymbol(
            new ProcedureNode("f", ImmutableList.of(), VarType.INT, BlockNode.EMPTY, null), null);
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(STACK_INT1, GLOBAL_INT1, null),
            new Call(
                GLOBAL_INT1,
                procSymbol,
                /* actuals= */ ImmutableList.of(),
                /* formals= */ ImmutableList.of(),
                null),
            new Return("proc", STACK_INT1));

    ImmutableList<Op> optimized = optimizer.optimize(program, null);
    assertThat(optimized.get(2)).isReturning(STACK_INT1);
  }
}
