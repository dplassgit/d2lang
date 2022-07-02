package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class RegisterStateTest {

  private Registers registers = new Registers();
  Emitter emitter = new ListEmitter();

  @Test
  public void condPushNothingAllocated() {
    // emitter is null, it should do nothing and not care.
    RegisterState.condPush(null, registers, ImmutableList.of());
  }

  @Test
  public void condPushSomethingAllocatedDontCare() {
    registers.allocate();
    // emitter is null, but since we don't care about RBX it shouldn't care.
    RegisterState.condPush(null, registers, ImmutableList.of());
  }

  @Test
  public void condPushSomethingAllocated() {
    Register register = registers.allocate();
    assertThat(register).isEqualTo(Register.RBX);
    RegisterState.condPush(emitter, registers, ImmutableList.of(register));
    emitter.emit("; hi");
    assertThat(emitter.all()).containsExactly("  push RBX", "; hi");
  }

  @Test
  public void condPopOne() {
    Register register = registers.allocate();
    assertThat(register).isEqualTo(Register.RBX);
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, ImmutableList.of(register));
    emitter.emit("; hi");
    registerState.condPop();
    assertThat(emitter.all()).containsExactly("  push RBX", "; hi", "  pop RBX");
  }

  @Test
  public void condPopMultiple() {
    // intentionally different order
    registers.reserve(Register.RDX);
    registers.reserve(Register.RCX);
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, ImmutableList.of(Register.RCX, Register.RDX));
    emitter.emit("; hi");
    registerState.condPop();
    assertThat(emitter.all())
        .containsExactly("  push RCX", "  push RDX", "; hi", "  pop RDX", "  pop RCX");
  }

  @Test
  public void pushed() {
    registers.reserve(Register.RDX);
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, ImmutableList.of(Register.RDX));
    assertThat(registerState.pushed(Register.RDX)).isTrue();
    assertThat(registerState.pushed(Register.RAX)).isFalse();
  }

  @Test
  public void manualPop() {
    registers.reserve(Register.RDX);
    registers.reserve(Register.RCX);
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, ImmutableList.of(Register.RDX, Register.RCX));
    registerState.condPop(Register.RCX);
    assertThat(emitter.all()).containsExactly("  push RCX", "  push RDX", "  pop RCX");
    assertThat(registerState.pushed(Register.RCX)).isFalse();
    registerState.condPop();
    assertThat(registerState.pushed(Register.RDX)).isFalse();
    assertThat(emitter.all()).containsExactly("  push RCX", "  push RDX", "  pop RCX", "  pop RDX");
  }
}
