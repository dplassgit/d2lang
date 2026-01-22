package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.codegen.il.testing.OpcodeSubject.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.StackLocation;
import com.plasstech.lang.d2.codegen.TempLocation;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Return;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.optimize.testing.OptimizerWithNop;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.VarType;

public class StringCompareOptimizerTest {
  private final Optimizer optimizer = new OptimizerWithNop(new StringCompareOptimizer(2));

  private final SymbolTable symTab = new SymbolTable();

  private static final TempLocation STRING_TEMP1 =
      LocationUtils.newTempLocation("stemp1", VarType.STRING);
  private static final TempLocation STRING_TEMP2 =
      LocationUtils.newTempLocation("stemp2", VarType.STRING);
  private static final TempLocation INT_TEMP1 = LocationUtils.newTempLocation("itemp", VarType.INT);
  private static final Location BOOL_TEMP1 = LocationUtils.newTempLocation("btemp1", VarType.BOOL);
  private static final ConstantOperand<String> CONSTANT_STRING = ConstantOperand.of("h");

  @Test
  public void compareIndex0() {
    // temp1 = s[0]
    // temp2 = temp1 == 'h'
    // becomes:
    // temp1 = asc(s)
    // temp3 = asc('h')
    // temp2 = temp1 == temp3
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(0), null),
            new BinOp(BOOL_TEMP1, STRING_TEMP1, TokenType.EQEQ, CONSTANT_STRING, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, symTab);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(3);

    assertThat(optimized.get(0)).isUnaryOpOf(TokenType.ASC, STRING_TEMP2);
    assertThat(optimized.get(1)).isUnaryOpOf(TokenType.ASC, CONSTANT_STRING);
    assertThat(optimized.get(2))
        .isBinOp(
            BOOL_TEMP1,
            optimized.get(0).getDestination(),
            TokenType.EQEQ,
            optimized.get(1).getDestination());
  }

  @Test
  public void nonConstRhs_does_not_optimize() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(0), null),
            new BinOp(
                BOOL_TEMP1,
                STRING_TEMP1,
                TokenType.EQEQ,
                LocationUtils.newMemoryAddress("s", VarType.STRING),
                null));

    optimizer.optimize(program, symTab);
    assertThat(optimizer).isNotChanged();
  }

  @Test
  public void compareIndex1_does_not_optimize() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(1), null),
            new BinOp(BOOL_TEMP1, STRING_TEMP1, TokenType.EQEQ, CONSTANT_STRING, null));

    optimizer.optimize(program, symTab);
    assertThat(optimizer).isNotChanged();
  }

  @Test
  public void compareIndexNonConstant_does_not_optimize() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, INT_TEMP1, null),
            new BinOp(BOOL_TEMP1, STRING_TEMP1, TokenType.EQEQ, CONSTANT_STRING, null));

    optimizer.optimize(program, symTab);
    assertThat(optimizer).isNotChanged();
  }

  @Test
  public void compareIndex0_does_not_disturb_later_ops() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(0), null),
            new BinOp(BOOL_TEMP1, STRING_TEMP1, TokenType.EQEQ, CONSTANT_STRING, null),
            new Return("proc"));

    ImmutableList<Op> optimized = optimizer.optimize(program, symTab);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(4);

    assertThat(optimized.get(3)).isInstanceOf(Return.class);
  }

  @Test
  public void nonTempDest_optimizes() {
    // temp1 = s[0]
    // nontemp = temp1 == 'h'
    // becomes:
    // temp1 = asc(s)
    // temp3 = asc('h')
    // nontemp = temp1 == temp3
    StackLocation localBool = LocationUtils.newStackLocation("dest", VarType.BOOL, 0);
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(0), null),
            new BinOp(localBool, STRING_TEMP1, TokenType.EQEQ, CONSTANT_STRING, null));

    ImmutableList<Op> optimized = optimizer.optimize(program, symTab);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(3);

    assertThat(optimized.get(0)).isUnaryOpOf(TokenType.ASC, STRING_TEMP2);
    assertThat(optimized.get(1)).isUnaryOpOf(TokenType.ASC, CONSTANT_STRING);
    assertThat(optimized.get(2))
        .isBinOp(
            localBool,
            optimized.get(0).getDestination(),
            TokenType.EQEQ,
            optimized.get(1).getDestination());
  }

  @Test
  public void nonTempintermediateDest_does_not_optimize() {
    StackLocation localBool = LocationUtils.newStackLocation("dest", VarType.BOOL, 0);
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(
                LocationUtils.newStackLocation("dest", VarType.STRING, 0),
                STRING_TEMP2,
                TokenType.LBRACKET,
                ConstantOperand.of(0),
                null),
            new BinOp(localBool, STRING_TEMP1, TokenType.EQEQ, CONSTANT_STRING, null));

    optimizer.optimize(program, symTab);
    assertThat(optimizer).isNotChanged();
  }

  @Test
  public void tooBigRhs_optimizes_eqeq() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(0), null),
            new BinOp(BOOL_TEMP1, STRING_TEMP1, TokenType.EQEQ, ConstantOperand.of("hi"), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, symTab);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.FALSE);
  }

  @Test
  public void tooBigRhs_optimizes_neq() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(0), null),
            new BinOp(BOOL_TEMP1, STRING_TEMP1, TokenType.NEQ, ConstantOperand.of("hi"), null));

    ImmutableList<Op> optimized = optimizer.optimize(program, symTab);
    assertThat(optimizer).isChanged();
    assertThat(optimized).hasSize(1);
    assertThat(optimized.get(0)).isTransferredFrom(ConstantOperand.TRUE);
  }

  @Test
  public void tooBigRhs_does_no_optimize_lt() {
    ImmutableList<Op> program =
        ImmutableList.of(
            new BinOp(STRING_TEMP1, STRING_TEMP2, TokenType.LBRACKET, ConstantOperand.of(0), null),
            new BinOp(BOOL_TEMP1, STRING_TEMP1, TokenType.LT, ConstantOperand.of("hi"), null));

    optimizer.optimize(program, symTab);
    assertThat(optimizer).isNotChanged();
  }
}
