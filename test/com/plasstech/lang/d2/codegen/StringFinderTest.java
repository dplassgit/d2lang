package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.Transfer;

public class StringFinderTest {
  private StringFinder sf = new StringFinder();

  @Test
  public void noCode() {
    ImmutableList<Op> ops = ImmutableList.of();
    StringTable table = sf.execute(ops);
    assertThat(table.entries()).isEmpty();
  }

  @Test
  public void noStrings() {
    ImmutableList<Op> ops = ImmutableList.of(new Transfer(null, ConstantOperand.of(true)));
    StringTable table = sf.execute(ops);
    assertThat(table.entries()).isEmpty();
  }

  @Test
  public void oneString() {
    ImmutableList<Op> ops = ImmutableList.of(new Transfer(null, ConstantOperand.of("hi")));
    StringTable table = sf.execute(ops);
    assertThat(table.entries()).hasSize(1);
  }

  @Test
  public void twoNonOverlappingStrings() {
    ImmutableList<Op> ops =
        ImmutableList.of(
            new Transfer(null, ConstantOperand.of("hi")),
            new Transfer(null, ConstantOperand.of("there")));
    StringTable table = sf.execute(ops);
    assertThat(table.entries()).hasSize(2);
  }

  @Test
  public void twoOverlappingStrings() {
    ImmutableList<Op> ops =
        ImmutableList.of(
            new Transfer(null, ConstantOperand.of("hi")),
            new Transfer(null, ConstantOperand.of("ohhi")));
    StringTable table = sf.execute(ops);
    // Still has size 2, though one references the other.
    assertThat(table.entries()).hasSize(2);
  }

  @Test
  public void twoIdenticalStrings() {
    ImmutableList<Op> ops =
        ImmutableList.of(
            new Transfer(null, ConstantOperand.of("hi")),
            new Transfer(null, ConstantOperand.of("hi")));
    StringTable table = sf.execute(ops);
    assertThat(table.entries()).hasSize(1);
  }
}
