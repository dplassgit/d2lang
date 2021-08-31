package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class RelativeStringConstantTest {
  private StringEntry base = new StringConstant("name", "value");
  private StringEntry relative = new RelativeStringConstant("relative", base, 3);

  @Test
  public void value() {
    assertThat(relative.value()).isEqualTo("ue");
  }

  @Test
  public void dataEntry() {
    assertThat(relative.dataEntry()).isEqualTo("relative EQU name+3");
  }
}
