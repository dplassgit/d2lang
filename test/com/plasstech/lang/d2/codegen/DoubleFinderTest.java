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
    assertThat(table.orderedEntries()).isEmpty();
  }

  @Test
  public void noStrings() {
    ImmutableList<Op> ops = ImmutableList.of(new Transfer(null, ConstantOperand.of(true)));
    DoubleTable table = df.execute(ops);
    assertThat(table.orderedEntries()).isEmpty();
  }

  @Test
  public void oneString() {
    ImmutableList<Op> ops = ImmutableList.of(new Transfer(null, ConstantOperand.of("hi")));
    DoubleTable table = df.execute(ops);
    assertThat(table.orderedEntries()).hasSize(1);
  }

  @Test
  public void twoNonOverlappingStrings() {
    ImmutableList<Op> ops =
        ImmutableList.of(
            new Transfer(null, ConstantOperand.of("hi")),
            new Transfer(null, ConstantOperand.of("there")));
    DoubleTable table = df.execute(ops);
    assertThat(table.orderedEntries()).hasSize(2);
  }

  @Test
  public void twoOverlappingStrings() {
    ImmutableList<Op> ops =
        ImmutableList.of(
            new Transfer(null, ConstantOperand.of("hi")),
            new Transfer(null, ConstantOperand.of("ohhi")));
    DoubleTable table = df.execute(ops);
    // Still has size 2, though one references the other.
    assertThat(table.orderedEntries()).hasSize(2);
  }

  @Test
  public void twoIdenticalStrings() {
    ImmutableList<Op> ops =
        ImmutableList.of(
            new Transfer(null, ConstantOperand.of("hi")),
            new Transfer(null, ConstantOperand.of("hi")));
    DoubleTable table = df.execute(ops);
    assertThat(table.orderedEntries()).hasSize(1);
  }
}
