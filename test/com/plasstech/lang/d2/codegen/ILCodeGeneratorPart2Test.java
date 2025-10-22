package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.SymbolStorage;
import com.plasstech.lang.d2.type.SymbolTable;
import com.plasstech.lang.d2.type.TypeCheckResult;
import com.plasstech.lang.d2.type.VarType;

public class ILCodeGeneratorPart2Test {
  private Phase part2 = new ILCodeGeneratorPart2();
  private SymbolTable symTab = new SymbolTable();
  private Location TEMP = LocationUtils.newTempLocation(symTab, "temp", VarType.STRING);
  private Operand NULL_OPERAND = new ConstantOperand<Void>(null, VarType.STRING);
  private Operand STRING_OPERAND = LocationUtils.newTempLocation(symTab, "left", VarType.STRING);
  private Location GLOBAL = LocationUtils.newMemoryAddress("dest", VarType.STRING);

  @Test
  public void noChange() {
    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(TEMP, STRING_OPERAND, TokenType.PLUS, NULL_OPERAND, null));
    State state = State.create().setIlCode(program);
    state = part2.execute(state);
    assertThat(state.ilCode()).isEqualTo(program);
  }

  @Test
  public void nullCoalesceTempDest() {
    long preAugmentTempcount = countVarsByStorage(SymbolStorage.LONG_TEMP);
    assertThat(preAugmentTempcount).isEqualTo(0);

    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(TEMP, STRING_OPERAND, TokenType.NULL_COALESCE, NULL_OPERAND, null));
    State state = State.create().setIlCode(program).addTypecheckResult(new TypeCheckResult(symTab));

    state = part2.execute(state);
    System.err.println(state.ilCode());

    assertThat(state.ilCode().size()).isGreaterThan(1);
    long postAugmentTempcount = countVarsByStorage(SymbolStorage.LONG_TEMP);
    assertThat(postAugmentTempcount).isGreaterThan(preAugmentTempcount);
  }

  @Test
  public void nullCoalesceDest() {
    long preAugmentTempcount = countVarsByStorage(SymbolStorage.LONG_TEMP);
    assertThat(preAugmentTempcount).isEqualTo(0);

    ImmutableList<Op> program = ImmutableList.of(
        new BinOp(GLOBAL, STRING_OPERAND, TokenType.NULL_COALESCE, NULL_OPERAND, null));
    State state = State.create().setIlCode(program).addTypecheckResult(new TypeCheckResult(symTab));

    state = part2.execute(state);
    System.err.println(state.ilCode());

    assertThat(state.ilCode().size()).isGreaterThan(1);
    long postAugmentTempcount = countVarsByStorage(SymbolStorage.LONG_TEMP);
    assertThat(postAugmentTempcount).isEqualTo(preAugmentTempcount);
  }

  private long countVarsByStorage(SymbolStorage symbolStorage) {
    return symTab.variables()
        .values()
        .stream()
        .filter(s -> s.storage() == symbolStorage)
        .count();
  }
}
