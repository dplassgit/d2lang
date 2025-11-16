package com.plasstech.lang.d2.codegen.x64;

import static com.google.common.truth.Truth.assertThat;

import com.plasstech.lang.d2.type.VarType;
import org.junit.Test;

public class RegistersTest {
  private Registers registers = new Registers();

  @Test
  public void allocate_all() {
    for (Register r : IntRegister.values()) {
      if (r != IntRegister.RAX) {
        assertThat(registers.allocate(VarType.INT)).isEqualTo(r);
      }
    }
    assertThat(registers.allocate(VarType.INT)).isNull();
  }

  @Test
  public void deallocate_one() {
    for (int i = 0; i < IntRegister.values().length; ++i) {
      registers.allocate(VarType.INT);
    }
    for (Register r : IntRegister.values()) {
      if (r != IntRegister.RAX) {
        registers.deallocate(r);
      }
    }
  }
}
