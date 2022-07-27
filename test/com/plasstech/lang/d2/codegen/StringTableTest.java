package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class StringTableTest {
  private final StringTable table = new StringTable();

  @Test
  public void add_one() {
    table.add("constant");
    ConstEntry<String> entry = table.lookup("constant");
    assertThat(entry.value()).isEqualTo("constant");
    assertThat(entry).isInstanceOf(StringConstant.class);
    assertThat(table.size()).isEqualTo(1);
  }

  @Test
  public void add_duplicate() {
    table.add("constant");
    table.add("constant");
    assertThat(table.size()).isEqualTo(1);
  }

  @Test
  public void add_duplicateLength() {
    table.add("a");
    table.add("b");
    assertThat(table.size()).isEqualTo(2);
  }

  @Test
  public void add_twoUnrelated() {
    table.add("constant");
    ConstEntry<String> entry = table.lookup("constant");
    assertThat(entry.value()).isEqualTo("constant");
    assertThat(entry).isInstanceOf(StringConstant.class);

    table.add("different");
    ConstEntry<String> entry2 = table.lookup("different");
    assertThat(entry2.value()).isEqualTo("different");
    assertThat(entry2).isInstanceOf(StringConstant.class);
  }

  @Test
  public void add_twoRelated() {
    table.add("constant");
    ConstEntry<String> entry = table.lookup("constant");
    assertThat(entry.value()).isEqualTo("constant");
    assertThat(entry).isInstanceOf(StringConstant.class);

    table.add("tant");
    entry = table.lookup("constant");
    RelativeStringConstant entry2 = (RelativeStringConstant) table.lookup("tant");
    assertThat(entry2.value()).isEqualTo("tant");
    assertThat(entry2.offset()).isEqualTo(4);
    assertThat(entry2.base()).isEqualTo(entry.name());
  }

  @Test
  public void add_twoRelatedReverse() {
    table.add("constant");
    ConstEntry<String> entry = table.lookup("constant");
    assertThat(entry.value()).isEqualTo("constant");
    assertThat(entry).isInstanceOf(StringConstant.class);

    table.add("unconstant");
    ConstEntry<String> entry2 = table.lookup("unconstant");
    assertThat(entry2.value()).isEqualTo("unconstant");
    assertThat(entry2).isInstanceOf(StringConstant.class);

    RelativeStringConstant entryAgain = (RelativeStringConstant) table.lookup("constant");
    assertThat(entryAgain.value()).isEqualTo("constant");
    assertThat(entryAgain.offset()).isEqualTo(2);
  }

  @Test
  public void add_threeRelated() {
    table.add("constant");
    table.add("t");
    table.add("tant");

    ConstEntry<String> entry = table.lookup("constant");
    RelativeStringConstant entry3 = (RelativeStringConstant) table.lookup("t");
    assertThat(entry3.value()).isEqualTo("t");
    assertThat(entry3.offset()).isEqualTo(7);
    assertThat(entry3.base()).isEqualTo(entry.name());
  }

  @Test
  public void lookup() {
    table.add("tant");
    table.add("t");
    table.add("constant");
    table.add("different");
    table.add("not different");
    table.add("differently");
    table.add("different from");

    ConstEntry<String> entry = table.lookup("constant");
    assertThat(entry.value()).isEqualTo("constant");
    ConstEntry<String> entry2 = table.lookup("tant");
    assertThat(entry2.value()).isEqualTo("tant");
    ConstEntry<String> entry3 = table.lookup("t");
    assertThat(entry3.value()).isEqualTo("t");
    ConstEntry<String> entry4 = table.lookup("different");
    assertThat(entry4.value()).isEqualTo("different");
  }

  @Test
  public void non_alphas() {
    table.add("tant");
    table.add("t, ant");
    table.add("t,ant");
    table.add("ant");

    System.err.println(table.entries());
    ConstEntry<String> entry = table.lookup("tant");
    assertThat(entry.value()).isEqualTo("tant");
    ConstEntry<String> entry2 = table.lookup("t, ant");
    assertThat(entry2.value()).isEqualTo("t, ant");
    ConstEntry<String> entry3 = table.lookup("t,ant");
    assertThat(entry3.value()).isEqualTo("t,ant");
    ConstEntry<String> entry4 = table.lookup("ant");
    assertThat(entry4.value()).isEqualTo("ant");
  }
}
