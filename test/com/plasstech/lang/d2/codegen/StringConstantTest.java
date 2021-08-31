package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class StringConstantTest {

  @Test
  public void dataEntry() {
    StringEntry entry = new StringConstant("name", "value");
    assertThat(entry.dataEntry()).isEqualTo("name: db \"value\", 0");
  }
}
