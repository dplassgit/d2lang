package com.plasstech.lang.d2.codegen.t100;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class T100DoubleDataTest {
  @Test
  public void doubleData() {
    T100DoubleData dd = new T100DoubleData("dbl", -2345.678);
    // 196, 35, 69, 103, 128, 0, 0, 0
    assertThat(dd.dataEntry()).isEqualTo("dbl: db 0xc4,0x23,0x45,0x67,0x80,0x00,0x00,0x00");
  }
}
