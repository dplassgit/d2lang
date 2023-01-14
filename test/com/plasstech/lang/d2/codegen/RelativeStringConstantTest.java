package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class RelativeStringConstantTest {
  private StringEntry base = new StringConstant("name", "valuevaluevaluevaluevalue");
  private StringEntry relative = new RelativeStringConstant("relative", base, 19);

  @Test
  public void value() {
    assertThat(relative.value()).isEqualTo("evalue");
  }

  @Test
  public void dataEntry() {
    assertThat(relative.dataEntry()).isEqualTo("relative EQU name+0x13");
  }

  @Test
  public void shortDataEntry() {
    StringEntry shortDifference = new RelativeStringConstant("shortDiff", base, 1);
    assertThat(shortDifference.dataEntry()).isEqualTo("shortDiff EQU name+0x01");
  }
}
