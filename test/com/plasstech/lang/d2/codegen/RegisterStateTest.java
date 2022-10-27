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
    assertThat(emitter.all()).containsAtLeast("  push RBX", "; hi");
  }

  @Test
  public void condPushDouble() {
    Register register = registers.allocate(VarType.DOUBLE);
    assertThat(register).isEqualTo(XmmRegister.XMM4);
    RegisterState.condPush(emitter, registers, ImmutableList.of(register));
    assertThat(emitter.all()).containsAtLeast("  sub RSP, 8", "  movq [RSP], XMM4").inOrder();
  }

  @Test
  public void condPopOne() {
    Register register = registers.allocate(VarType.INT);
    assertThat(register).isEqualTo(IntRegister.RBX);
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, ImmutableList.of(register));
    emitter.emit0("; hi");
    registerState.condPop();
    assertThat(emitter.all()).containsAtLeast("  push RBX", "; hi", "  pop RBX").inOrder();
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
        .containsAtLeast("  push RCX", "  push RDX", "; hi", "  pop RDX", "  pop RCX")
        .inOrder();
  }

  @Test
  public void condPopDouble() {
    registers.reserve(XmmRegister.XMM1);
    registers.reserve(IntRegister.RCX);
    RegisterState registerState =
        RegisterState.condPush(
            emitter, registers, ImmutableList.of(IntRegister.RCX, XmmRegister.XMM1));
    registerState.condPop();
    assertThat(emitter.all())
        .containsAtLeast(
            "  push RCX",
            "  sub RSP, 8",
            "  movq [RSP], XMM1",
            "  movq XMM1, [RSP]",
            "  add RSP, 8",
            "  pop RCX")
        .inOrder();
  }

  @Test
  public void pushed() {
    registers.reserve(IntRegister.RDX);
    RegisterState registerState =
        RegisterState.condPush(emitter, registers, ImmutableList.of(IntRegister.RDX));
    assertThat(registerState.wasPushed(IntRegister.RDX)).isTrue();
    assertThat(registerState.wasPushed(IntRegister.RAX)).isFalse();
  }

  @Test
  public void manualPop() {
    registers.reserve(IntRegister.RDX);
    registers.reserve(IntRegister.RCX);
    RegisterState registerState =
        RegisterState.condPush(
            emitter, registers, ImmutableList.of(IntRegister.RDX, IntRegister.RCX));
    registerState.condPop(IntRegister.RCX);
    assertThat(emitter.all()).containsAtLeast("  push RDX", "  push RCX", "  pop RCX").inOrder();
    assertThat(registerState.wasPushed(IntRegister.RCX)).isFalse();
    registerState.condPop();
    assertThat(registerState.wasPushed(IntRegister.RDX)).isFalse();
    assertThat(emitter.all())
        .containsAtLeast("  push RDX", "  push RCX", "  pop RCX", "  pop RDX")
        .inOrder();
  }
}
