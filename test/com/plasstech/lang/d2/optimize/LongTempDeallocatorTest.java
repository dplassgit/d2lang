package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.DeallocateTemp;
import com.plasstech.lang.d2.codegen.il.Nop;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;
import com.plasstech.lang.d2.codegen.testing.LocationUtils;
import com.plasstech.lang.d2.common.TokenType;

public class LongTempDeallocatorTest {
  private static final Location A = LocationUtils.newParamLocation("a", null, 0, 0);
  private static final Location B = LocationUtils.newParamLocation("b", null, 0, 0);
  private static final Location C = LocationUtils.newParamLocation("c", null, 0, 0);
  private static final Location LONG_TEMP = LocationUtils.newLongTempLocation("longTemp", null);
  private static final Location LONG_TEMP2 = LocationUtils.newLongTempLocation("longTemp2", null);
  private Optimizer deallocator = new LongTempDeallocator();

  @Test
  public void noCode_doesntAddDealloc() {
    ImmutableList<Op> output = execute(ImmutableList.of());
    assertThat(deallocator.isChanged()).isFalse();
    assertThat(output).isEmpty();
  }

  @Test
  public void noLongTemps_doesntAddDealloc() {
    ImmutableList<Op> code = ImmutableList.of(new BinOp(A, B, TokenType.PLUS, C, null));
    ImmutableList<Op> output = execute(code);
    assertThat(deallocator.isChanged()).isFalse();
    assertThat(output).isEqualTo(code);
  }

  @Test
  public void longTempDestinationOnly_nops() {
    ImmutableList<Op> code = ImmutableList.of(new BinOp(LONG_TEMP, B, TokenType.PLUS, C, null));
    ImmutableList<Op> output = execute(code);
    assertThat(deallocator.isChanged()).isTrue();
    assertThat(output.get(0)).isInstanceOf(Nop.class);
  }

  @Test
  public void oneLongTemp_addsDealloc() {
    ImmutableList<Op> code = ImmutableList.of(
        new Transfer(LONG_TEMP, B, null),
        new BinOp(A, B, TokenType.PLUS, C, null),
        new Transfer(A, LONG_TEMP, null));

    ImmutableList<Op> output = execute(code);
    //    assertThat(deallocator.isChanged()).isTrue();

    assertThat(output).hasSize(4);
    assertThat(output.get(0)).isEqualTo(code.get(0));
    assertThat(output.get(1)).isEqualTo(code.get(1));
    assertThat(output.get(2)).isEqualTo(code.get(2));
    assertThat(output.get(3)).isInstanceOf(DeallocateTemp.class);
  }

  @Test
  public void longTempUsedMultipleTimes_addsOneDealloc() {
    ImmutableList<Op> code = ImmutableList.of(
        new BinOp(A, B, TokenType.PLUS, LONG_TEMP, null),
        new Transfer(A, LONG_TEMP, null));

    ImmutableList<Op> output = execute(code);

    assertThat(output).hasSize(3);
    assertThat(output.get(0)).isEqualTo(code.get(0));
    assertThat(output.get(1)).isEqualTo(code.get(1));
    DeallocateTemp op = (DeallocateTemp) output.get(2);
    assertThat(op.temp()).isEqualTo(LONG_TEMP);
  }

  @Test
  public void multipleLongTemps_addsOneDeallocEach() {
    ImmutableList<Op> code = ImmutableList.of(
        new BinOp(A, B, TokenType.PLUS, LONG_TEMP, null),
        new Transfer(A, LONG_TEMP2, null));

    ImmutableList<Op> output = execute(code);
    //    assertThat(deallocator.isChanged()).isTrue();

    assertThat(output).hasSize(4);
    assertThat(output.get(0)).isEqualTo(code.get(0));

    DeallocateTemp op = (DeallocateTemp) output.get(1);
    assertThat(op.temp()).isEqualTo(LONG_TEMP);

    assertThat(output.get(2)).isEqualTo(code.get(1));

    DeallocateTemp op2 = (DeallocateTemp) output.get(3);
    assertThat(op2.temp()).isEqualTo(LONG_TEMP2);
  }

  private ImmutableList<Op> execute(ImmutableList<Op> code) {
    return deallocator.optimize(code, null);
  }
}
