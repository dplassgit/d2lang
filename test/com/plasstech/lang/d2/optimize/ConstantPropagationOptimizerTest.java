package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;
import static com.plasstech.lang.d2.optimize.OpcodeSubject.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.MemoryAddress;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.VarType;

public class ConstantPropagationOptimizerTest {
  private static final Optimizer OPTIMIZER =
      new ILOptimizer(ImmutableList.of(new ConstantPropagationOptimizer(2), new NopOptimizer()))
          .setDebugLevel(2);

  private static final TempLocation TEMP_INT1 = LocationUtils.newTempLocation("__temp1", VarType.INT);
  private static final TempLocation TEMP_INT2 = LocationUtils.newTempLocation("__temp2", VarType.INT);
  private static final StackLocation STACK_INT1 =
      LocationUtils.newStackLocation("s1", VarType.INT, 0);
  private static final MemoryAddress GLOBAL_INT1 =
      LocationUtils.newMemoryAddress("g1", VarType.INT);
  private static final MemoryAddress GLOBAL_INT2 =
      LocationUtils.newMemoryAddress("g2", VarType.INT);

  @Test
  public void twoTransfers() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP_INT1, ConstantOperand.ONE, null),
            new Transfer(TEMP_INT2, TEMP_INT1, null),
            new Transfer(STACK_INT1, TEMP_INT2, null));

    program = OPTIMIZER.optimize(program, null);
    assertThat(program).hasSize(1);
    assertThat(program.get(0)).isTransferredFrom(ConstantOperand.ONE);
  }

  @Test
  public void transferToGlobal() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(GLOBAL_INT1, ConstantOperand.ONE, null),
            new Transfer(TEMP_INT1, GLOBAL_INT1, null), //
            new Transfer(GLOBAL_INT2, TEMP_INT1, null));

    program = OPTIMIZER.optimize(program, null);
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

    program = OPTIMIZER.optimize(program, null);
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

    program = OPTIMIZER.optimize(program, null);
    assertThat(program).hasSize(3);
    Return returnOp = (Return) program.get(2);
    assertThat(returnOp.returnValueLocation()).hasValue(GLOBAL_INT1);
  }
}
