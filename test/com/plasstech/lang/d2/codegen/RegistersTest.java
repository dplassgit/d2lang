package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class RegistersTest {

  @Test
  public void allocate_all() {
    Registers r = new Registers();
    for (int i = 0; i < 31; ++i) {
      assertThat(r.allocate()).isEqualTo(i + 1);
    }
  }

  @Test
  public void deallocate_one() {
    Registers registers = new Registers();
    for (int i = 0; i < 10; ++i) {
      assertThat(registers.allocate()).isEqualTo(1);
      registers.deallocate(1);
    }
  }
}
