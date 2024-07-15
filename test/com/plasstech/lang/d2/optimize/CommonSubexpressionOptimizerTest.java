package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.OpcodeSubject.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ConstantOperand;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.VarType;

public class CommonSubexpressionOptimizerTest {

  private static final Location A = LocationUtils.newParamLocation("A", VarType.INT, 0, 0);
  private static final Location B = LocationUtils.newParamLocation("B", VarType.INT, 0, 0);
  private static final Location C = LocationUtils.newParamLocation("C", VarType.INT, 0, 0);
  private static final Location D = LocationUtils.newParamLocation("D", VarType.INT, 0, 0);
  private static final Location TEMP = LocationUtils.newTempLocation("temp", VarType.INT);
  private static final Location LONG_TEMP = LocationUtils.newLongTempLocation("longtemp", null);

  private Optimizer optimizer = new CommonSubexpressionOptimizer(2);

  @Test
  public void allParams() {
    ImmutableList<Op> code = ImmutableList.of(
        new BinOp(A, B, TokenType.PLUS, C, null),
        new BinOp(D, B, TokenType.PLUS, C, null));
    var optimized = optimizer.optimize(code, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    assertThat(optimized.get(1)).isTransferredFrom(A);
  }

  @Test
  public void constants() {
    ImmutableList<Op> code = ImmutableList.of(
        new BinOp(A, B, TokenType.PLUS, ConstantOperand.ONE, null),
        new BinOp(D, B, TokenType.PLUS, ConstantOperand.ONE, null));
    var optimized = optimizer.optimize(code, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(2);
    assertThat(optimized.get(1)).isTransferredFrom(A);
  }

  @Test
  public void allParamsStops() {
    ImmutableList<Op> code = ImmutableList.of(
        new BinOp(A, B, TokenType.PLUS, C, null),
        new BinOp(D, B, TokenType.PLUS, C, null),
        new Transfer(B, C, null));
    var optimized = optimizer.optimize(code, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(3);
    assertThat(optimized.get(1)).isTransferredFrom(A);
    assertThat(optimized.get(2)).isTransferredFrom(C);
  }

  @Test
  public void allParamsIgnoresUnrelated() {
    ImmutableList<Op> code = ImmutableList.of(
        new BinOp(A, B, TokenType.PLUS, C, null),
        new Transfer(TEMP, C, null),
        new BinOp(D, B, TokenType.PLUS, C, null));
    var optimized = optimizer.optimize(code, null);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(3);
    assertThat(optimized.get(2)).isTransferredFrom(A);
  }

  @Test
  public void stopWhenSourceChanged() {
    ImmutableList<Op> code = ImmutableList.of(
        new BinOp(A, B, TokenType.PLUS, C, null),
        new Transfer(B, C, null),
        new BinOp(D, B, TokenType.PLUS, C, null));
    optimizer.optimize(code, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void stopWhenDestChanged() {
    ImmutableList<Op> code = ImmutableList.of(
        new BinOp(A, B, TokenType.PLUS, C, null),
        new Transfer(A, C, null),
        new BinOp(D, B, TokenType.PLUS, C, null));
    optimizer.optimize(code, null);
    assertThat(optimizer.isChanged()).isFalse();
  }

  @Test
  public void tempDest_becomesLongTemp() {
    ImmutableList<Op> code = ImmutableList.of(
        new BinOp(TEMP, B, TokenType.PLUS, C, null),
        new BinOp(D, B, TokenType.PLUS, C, null),
        new BinOp(B, D, TokenType.PLUS, TEMP, null));
    // Should become:
    //  longtemp = b+c
    //  d=longtemp
    //  b=d+longtemp

    List<Op> optimized = optimizer.optimize(code, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(3);

    BinOp newFirst = (BinOp) optimized.get(0);
    var newLongTemp = newFirst.destination();
    assertThat(newLongTemp.storage()).isEqualTo(SymbolStorage.LONG_TEMP);

    assertThat(optimized.get(0)).isBinOp(newLongTemp, B, TokenType.PLUS, C);
    assertThat(optimized.get(1)).isTransferredFrom(newLongTemp);
    assertThat(optimized.get(2)).isBinOp(B, D, TokenType.PLUS, newLongTemp);
  }

  @Test
  public void unaryTempDest_becomesLongTemp() {
    ImmutableList<Op> code = ImmutableList.of(
        new UnaryOp(TEMP, TokenType.MINUS, C, null),
        new UnaryOp(D, TokenType.MINUS, C, null),
        new BinOp(B, D, TokenType.PLUS, TEMP, null));
    // Should become:
    //  longtemp = -c
    //  d=longtemp
    //  b=d+longtemp

    List<Op> optimized = optimizer.optimize(code, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(3);

    UnaryOp newFirst = (UnaryOp) optimized.get(0);
    var newLongTemp = newFirst.destination();
    assertThat(newLongTemp.storage()).isEqualTo(SymbolStorage.LONG_TEMP);

    assertThat(optimized.get(0)).isUnaryOp(newLongTemp, TokenType.MINUS, C);
    assertThat(optimized.get(1)).isTransferredFrom(newLongTemp);
    assertThat(optimized.get(2)).isBinOp(B, D, TokenType.PLUS, newLongTemp);
  }

  @Test
  public void longTempDest() {
    ImmutableList<Op> code = ImmutableList.of(
        new BinOp(LONG_TEMP, B, TokenType.PLUS, C, null),
        new Transfer(D, LONG_TEMP, null),
        new BinOp(D, B, TokenType.PLUS, C, null));
    // Should become:
    //  longtemp = b+c
    //  d=longtemp
    //  d=longtemp

    List<Op> optimized = optimizer.optimize(code, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(3);

    // no change
    assertThat(optimized.get(0)).isEqualTo(code.get(0));
    assertThat(optimized.get(1)).isTransferredFrom(LONG_TEMP);
    assertThat(optimized.get(2)).isTransferredFrom(LONG_TEMP);
  }

  @Test
  public void unaryLongTempDest() {
    ImmutableList<Op> code = ImmutableList.of(
        new UnaryOp(LONG_TEMP, TokenType.MINUS, C, null),
        new Transfer(D, LONG_TEMP, null),
        new UnaryOp(D, TokenType.MINUS, C, null));
    // Should become:
    //  longtemp = -c
    //  d=longtemp
    //  d=longtemp

    List<Op> optimized = optimizer.optimize(code, null);

    assertThat(optimizer.isChanged()).isTrue();
    assertThat(optimized).hasSize(3);

    // no change
    assertThat(optimized.get(0)).isEqualTo(code.get(0));
    assertThat(optimized.get(1)).isTransferredFrom(LONG_TEMP);
    assertThat(optimized.get(2)).isTransferredFrom(LONG_TEMP);
  }
}
