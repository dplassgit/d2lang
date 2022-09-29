package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.OpcodeSubject.assertThat;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.MemoryAddress;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.type.VarType;

public class ConstantPropagationOptimizerTest {
  private static final Optimizer OPTIMIZER =
      new ILOptimizer(ImmutableList.of(new ConstantPropagationOptimizer(2), new NopOptimizer()))
          .setDebugLevel(2);

  private static final TempLocation TEMP_INT1 = new TempLocation("__temp1", VarType.INT);
  private static final TempLocation TEMP_INT2 = new TempLocation("__temp2", VarType.INT);
  private static final StackLocation STACK_INT1 = new StackLocation("s1", VarType.INT, 0);
  private static final MemoryAddress GLOBAL_INT1 = new MemoryAddress("g1", VarType.INT);
  private static final MemoryAddress GLOBAL_INT2 = new MemoryAddress("g2", VarType.INT);

  @Test
  public void twoTransfers() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(TEMP_INT1, ConstantOperand.ONE),
            new Transfer(TEMP_INT2, TEMP_INT1),
            new Transfer(STACK_INT1, TEMP_INT2));

    program = OPTIMIZER.optimize(program, null);

    assertThat(program.get(0)).isTransferredFrom(ConstantOperand.ONE);
  }

  @Test
  @Ignore("bug#93: Do some limited constant propagation of globals")
  public void transferToGlobal() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new Transfer(GLOBAL_INT1, ConstantOperand.ONE),
            new Transfer(TEMP_INT1, GLOBAL_INT1), //
            new Transfer(GLOBAL_INT2, TEMP_INT1));

    program = OPTIMIZER.optimize(program, null);
    assertThat(program).hasSize(2);
    assertThat(program.get(0)).isTransferredFrom(ConstantOperand.ONE);
    assertThat(program.get(1)).isTransferredFrom(GLOBAL_INT1);
  }
}
