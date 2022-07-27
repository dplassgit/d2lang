package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;

public class DoubleFinderTest {
  private DoubleFinder df = new DoubleFinder();

  @Test
  public void noCode() {
    ImmutableList<Op> ops = ImmutableList.of();
    DoubleTable table = df.execute(ops);
    assertThat(table.entries()).isEmpty();
  }

  @Test
  public void noDoubles() {
    ImmutableList<Op> ops = ImmutableList.of(new Transfer(null, ConstantOperand.of(true)));
    DoubleTable table = df.execute(ops);
    assertThat(table.entries()).isEmpty();
  }

  @Test
  public void oneDouble() {
    ImmutableList<Op> ops = ImmutableList.of(new Transfer(null, ConstantOperand.of(3.14)));
    DoubleTable table = df.execute(ops);
    assertThat(table.entries()).hasSize(1);
  }

  @Test
  public void twoDoubles() {
    ImmutableList<Op> ops =
        ImmutableList.of(
            new Transfer(null, ConstantOperand.of(3.14)),
            new Transfer(null, ConstantOperand.of(5.1)));
    DoubleTable table = df.execute(ops);
    assertThat(table.entries()).hasSize(2);
  }

  @Test
  public void twoDoublesAndOthers() {
    ImmutableList<Op> ops =
        ImmutableList.of(
            new Transfer(null, ConstantOperand.of(3.14)),
            new Transfer(null, ConstantOperand.of("hi")),
            new Transfer(null, ConstantOperand.of(3)),
            new Transfer(null, ConstantOperand.of(true)),
            new Transfer(null, ConstantOperand.of(5.1)));
    DoubleTable table = df.execute(ops);
    assertThat(table.entries()).hasSize(2);
  }

  @Test
  public void twoIdenticalDoubles() {
    ImmutableList<Op> ops =
        ImmutableList.of(
            new Transfer(null, ConstantOperand.of(3.14)),
            new Transfer(null, ConstantOperand.of(3.14)));
    DoubleTable table = df.execute(ops);
    assertThat(table.entries()).hasSize(1);
    ConstEntry<Double> entry = table.lookup(3.14);
    assertThat(entry.value()).isEqualTo(3.14);
  }
}
