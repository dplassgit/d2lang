package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.type.VarType;

public class RegisterStateTest {

  private Registers registers = new Registers();
  private Emitter emitter = new ListEmitter();

  @Test
  public void condPushNothingAllocated() {
    // emitter is null, it should do nothing and not care.
    RegisterState.condPush(null, registers, ImmutableList.of());
  }

  @Test
  public void condPushSomethingAllocatedDontCare() {
    registers.allocate(VarType.INT);
    // emitter is null, but since we don't care about RBX it shouldn't care.
    RegisterState.condPush(null, registers, ImmutableList.of());
  }

  @Test
  public void condPushSomethingAllocated() {
    Register register = registers.allocate(VarType.INT);
    assertThat(register).isEqualTo(IntRegister.RBX);
    RegisterState.condPush(emitter, registers, ImmutableList.of(register));
    emitter.emit0("; hi");
    assertThat(emitter.all()).containsExactly("  push RBX", "; hi");
  }

  @Test
  public void condPushDouble() {
    Register register = registers.allocate(VarType.DOUBLE);
    assertThat(register).isEqualTo(MmxRegister.XMM0);
    RegisterState.condPush(emitter, registers, ImmutableList.of(register));
    assertThat(emitter.all()).containsExactly("  sub RSP, 0x10", "  movdqu [RSP], XMM0");
  }

  @Test
  public void condPopOne() {
    Register register = registers.allocate(VarType.INT);
    assertThat(register).isEqualTo(IntRegister.RBX);
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, ImmutableList.of(register));
    emitter.emit0("; hi");
    registerState.condPop();
    assertThat(emitter.all()).containsExactly("  push RBX", "; hi", "  pop RBX");
  }

  @Test
  public void condPopMultiple() {
    // intentionally different order
    registers.reserve(IntRegister.RDX);
    registers.reserve(IntRegister.RCX);
    RegisterState registerState =
        RegisterState.condPush(
            emitter, registers, ImmutableList.of(IntRegister.RCX, IntRegister.RDX));
    emitter.emit0("; hi");
    registerState.condPop();
    assertThat(emitter.all())
        .containsExactly("  push RCX", "  push RDX", "; hi", "  pop RDX", "  pop RCX");
  }

  @Test
  public void condPopDouble() {
    registers.reserve(MmxRegister.XMM1);
    registers.reserve(IntRegister.RCX);
    RegisterState registerState =
        RegisterState.condPush(
            emitter, registers, ImmutableList.of(IntRegister.RCX, MmxRegister.XMM1));
    registerState.condPop();
    assertThat(emitter.all())
        .containsExactly(
            "  push RCX",
            "  sub RSP, 0x10",
            "  movdqu [RSP], XMM1",
            "  movdqu XMM1, [RSP]",
            "  add RSP, 0x10",
            "  pop RCX");
  }

  @Test
  public void pushed() {
    registers.reserve(IntRegister.RDX);
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, ImmutableList.of(IntRegister.RDX));
    assertThat(registerState.pushed(IntRegister.RDX)).isTrue();
    assertThat(registerState.pushed(IntRegister.RAX)).isFalse();
  }

  @Test
  public void manualPop() {
    registers.reserve(IntRegister.RDX);
    registers.reserve(IntRegister.RCX);
    RegisterState registerState =
        RegisterState.condPush(
            emitter, registers, ImmutableList.of(IntRegister.RDX, IntRegister.RCX));
    registerState.condPop(IntRegister.RCX);
    assertThat(emitter.all()).containsExactly("  push RCX", "  push RDX", "  pop RCX");
    assertThat(registerState.pushed(IntRegister.RCX)).isFalse();
    registerState.condPop();
    assertThat(registerState.pushed(IntRegister.RDX)).isFalse();
    assertThat(emitter.all()).containsExactly("  push RCX", "  push RDX", "  pop RCX", "  pop RDX");
  }
}
