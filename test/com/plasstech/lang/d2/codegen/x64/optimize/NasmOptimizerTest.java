package com.plasstech.lang.d2.codegen.x64.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;

@RunWith(TestParameterInjector.class)
public class NasmOptimizerTest {

  private Phase optimizer = new NasmOptimizer();

  @Test
  public void removesComments() {
    ImmutableList<String> code = ImmutableList.of(";", "; comment");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEmpty();
  }

  @Test
  public void removesEmptyLines() {
    ImmutableList<String> code = ImmutableList.of(" ", "");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEmpty();
  }

  @Test
  public void removesDeadJmp() {
    ImmutableList<String> code = ImmutableList.of(" jmp _next", "_next:");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly("_next:");
  }

  @Test
  public void removesDeadJmpWithCommentBetween() {
    ImmutableList<String> code = ImmutableList.of(" jmp _next", ";", "_next:");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly("_next:");
  }

  @Test
  public void leavesLiveJmp() {
    ImmutableList<String> code = ImmutableList.of(" jmp _next", "  mov eax, 0", "_next:");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEqualTo(code);
  }

  @Test
  public void removesJmpAfterRet() {
    ImmutableList<String> code = ImmutableList.of("  ret", " jmp _next");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly("  ret");
  }

  @Test
  public void replacesAdd1WithInc() {
    ImmutableList<String> code = ImmutableList.of("  add DWORD ECX, 1");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly("  inc DWORD ECX");
  }

  @Test
  public void replacesAdd1WithInc_comment() {
    ImmutableList<String> code = ImmutableList.of("  add DWORD ECX, 1  ; comment");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly("  inc DWORD ECX");
  }

  @Test
  public void replacesSub1WithDec() {
    ImmutableList<String> code = ImmutableList.of("  sub BYTE CL, 1");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly("  dec BYTE CL");
  }

  @Test
  public void replacesQwordAdd1WithInc() {
    ImmutableList<String> code = ImmutableList.of("  add QWORD [RBP + 28], 1");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly("  inc QWORD [RBP + 28]");
  }

  @Test
  public void leavesAdd10Comment() {
    ImmutableList<String> code = ImmutableList.of("  add RBX, 10  ; get");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly("  add RBX, 10  ");
  }

  @Test
  public void leavesAdd10() {
    ImmutableList<String> code = ImmutableList.of("  add RBX, 10");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEqualTo(code);
  }

  @Test
  public void localAdd10() {
    ImmutableList<String> code = ImmutableList.of("  add [RBP + 16], 10");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEqualTo(code);
  }

  @Test
  public void localAdd1() {
    ImmutableList<String> code = ImmutableList.of("  add [RBP + 16], 1");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly("  inc [RBP + 16]");
  }

  @Test
  public void leavesAddNot1() {
    ImmutableList<String> code = ImmutableList.of("  add DWORD RCX, 2");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEqualTo(code);
  }

  @Test
  public void removesAddSubRsp_sameOffset(
      @TestParameter({"0x20", "0x28"}) String offset) {
    ImmutableList<String> code = ImmutableList.of("  add RSP, " + offset, "  sub RSP, " + offset);
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEmpty();
  }

  @Test
  public void leavesAddSubRsp_diffOffset() {
    ImmutableList<String> code = ImmutableList.of("  add RSP, 0x20", "  sub RSP, 0x28");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEqualTo(code);
  }

  @Test
  public void leavesAddRsp_noSub() {
    ImmutableList<String> code = ImmutableList.of("  add RSP, 0x20", "  pop RCX");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEqualTo(code);
  }

  @Test
  public void replacesMovReg_0_with_xor(
      @TestParameter(
        {"RAX", "EBX", "CX", "DL", "BH", "R0", "R8", "R10", "R15d", "R14b", "RSI", "ESI", "SI",
            "SIL", "RDI", "EDI", "DI", "DIL"}
      ) String register,
      @TestParameter({" ", "BYTE ", "DWORD ", "QWORD "}) String modifier) {

    ImmutableList<String> code =
        ImmutableList.of(String.format("  mov %s%s, 0", modifier, register));
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly(String.format("  xor %s, %s", register, register));
  }

  @Test
  public void replacesMovReg_0_with_xor_no_modifier(
      @TestParameter(
        {"RAX", "EBX", "CX", "DL", "BH", "R0", "R8", "R10", "R15d", "R14b", "RSI", "ESI", "SI",
            "SIL", "RDI", "EDI", "DI", "DIL"}
      ) String register) {

    ImmutableList<String> code =
        ImmutableList.of(String.format("  mov %s, 0", register));
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly(String.format("  xor %s, %s", register, register));
  }

  @Test
  public void replacesMovReg_0_with_xorWithFarComment() {
    ImmutableList<String> code = ImmutableList.of("  mov EAX, 0    ;   do it");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly(String.format("  xor EAX, EAX"));
  }

  @Test
  public void replacesMovReg_0_with_xorWithNearComment() {
    ImmutableList<String> code = ImmutableList.of("  mov EAX, 0; do it");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).containsExactly(String.format("  xor EAX, EAX"));
  }

  @Test
  public void leavesMovReg_0x() {
    ImmutableList<String> code = ImmutableList.of("  mov RAX, 0x20");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEqualTo(code);
  }

  @Test
  public void leavesMovBracketReg() {
    ImmutableList<String> code = ImmutableList.of("  mov BYTE [RAX + 1], 0");
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    assertThat(state.asmCode()).isEqualTo(code);
  }
}
