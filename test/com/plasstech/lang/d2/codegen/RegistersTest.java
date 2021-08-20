package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class RegistersTest {
  private Registers registers = new Registers();

  @Test
  public void allocate_all() {
    for (Register r : Register.values()) {
      assertThat(registers.allocate()).isEqualTo(r);
    }
    assertThat(registers.allocate()).isNull();
  }

  @Test
  public void deallocate_one() {
    for (int i = 0; i < Register.values().length; ++i) {
      registers.allocate();
    }
    for (Register r : Register.values()) {
      registers.deallocate(r);
    }
  }
}
