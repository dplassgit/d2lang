package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import com.plasstech.lang.d2.common.D2RuntimeException;

public class RegistersTest {
  private Registers registers = new Registers();

  @Test
  public void allocate_all() {
    for (Register r : IntRegister.values()) {
      assertThat(registers.allocate()).isEqualTo(r);
    }
    assertThrows(D2RuntimeException.class, () -> registers.allocate());
  }

  @Test
  public void deallocate_one() {
    for (int i = 0; i < IntRegister.values().length; ++i) {
      registers.allocate();
    }
    for (Register r : IntRegister.values()) {
      registers.deallocate(r);
    }
  }
}
