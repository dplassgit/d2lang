package com.plasstech.lang.d2.codegen.x64.optimize;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.State;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class NasmOptimizerTest {

  private Phase optimizer = new NasmOptimizer(2);

  private ImmutableList<String> optimize(ImmutableList<String> code) {
    State state = State.create().addAsmCode(code);
    state = optimizer.execute(state);
    return state.asmCode();
  }

  @Test
  public void removesComments() {
    ImmutableList<String> code = ImmutableList.of(";", "; comment");
    assertThat(optimize(code)).isEmpty();
  }

  @Test
  public void retainsStringConstants() {
    String input =
        "  ARRAY_INDEX_NEGATIVE_ERR: db \"Invalid index err non-negative; was %d\", 10, 0";
    ImmutableList<String> code = ImmutableList.of(input);
    assertThat(optimize(code)).isEqualTo(code);
  }

  @Test
  public void removesEmptyLines() {
    ImmutableList<String> code = ImmutableList.of(" ", "");
    assertThat(optimize(code)).isEmpty();
  }

  @Test
  public void removesDeadJmp() {
    ImmutableList<String> code = ImmutableList.of(" jmp _next", "_next:");
    assertThat(optimize(code)).containsExactly("_next:");
  }

  @Test
  public void removesDeadJmpWithCommentBetween() {
    ImmutableList<String> code = ImmutableList.of(" jmp _next", ";", "_next:");
    assertThat(optimize(code)).containsExactly("_next:");
  }

  @Test
  public void leavesLiveJmp() {
    ImmutableList<String> code = ImmutableList.of(" jmp _next", "  mov eax, 0", "_next:");
    assertThat(optimize(code)).isEqualTo(code);
  }

  @Test
  public void removesJmpAfterRet() {
    ImmutableList<String> code = ImmutableList.of("  ret", " jmp _next");
    assertThat(optimize(code)).containsExactly("  ret");
  }

  @Test
  public void removesCommentFromCmp() {
    ImmutableList<String> code =
        ImmutableList.of("  cmp DWORD [_NUM_PLANETS], 0  ; direct comparison");
    assertThat(optimize(code)).containsExactly("  cmp DWORD [_NUM_PLANETS], 0");
  }

  @Test
  public void leavesAdd10() {
    ImmutableList<String> code = ImmutableList.of("  add RBX, 10");
    assertThat(optimize(code)).isEqualTo(code);
  }

  @Test
  public void leavesAdd10Comment() {
    ImmutableList<String> code = ImmutableList.of("  add RBX, 10  ; get");
    assertThat(optimize(code)).containsExactly("  add RBX, 10");
  }

  @Test
  public void trimsAdd10() {
    ImmutableList<String> code = ImmutableList.of("  add RBX, 10  ");
    assertThat(optimize(code)).containsExactly("  add RBX, 10");
  }

  @Test
  public void localAdd10() {
    ImmutableList<String> code = ImmutableList.of("  add [RBP + 16], 10");
    assertThat(optimize(code)).isEqualTo(code);
  }

  @Test
  public void localAdd1() {
    ImmutableList<String> code = ImmutableList.of("  add BYTE [RBP + 16], 1");
    assertThat(optimize(code)).containsExactly("  inc BYTE [RBP + 16]");
  }

  @Test
  public void leavesAddNot1() {
    ImmutableList<String> code = ImmutableList.of("  add DWORD RCX, 2");
    assertThat(optimize(code)).isEqualTo(code);
  }

  @Test
  public void removesAddSubRsp_sameOffset(@TestParameter({"0x20", "0x28"}) String offset) {
    ImmutableList<String> code = ImmutableList.of("  add RSP, " + offset, "  sub RSP, " + offset);
    assertThat(optimize(code)).isEmpty();
  }

  @Test
  public void leavesAddSubRsp_diffOffset() {
    ImmutableList<String> code = ImmutableList.of("  add RSP, 0x20", "  sub RSP, 0x28");
    assertThat(optimize(code)).containsExactly("  sub RSP, 0x08");
  }

  @Test
  public void leavesAddRsp_noSub() {
    ImmutableList<String> code = ImmutableList.of("  add RSP, 0x20", "  pop RCX");
    assertThat(optimize(code)).isEqualTo(code);
  }

  @Test
  public void leavesMovReg_0x() {
    ImmutableList<String> code = ImmutableList.of("  mov RAX, 0x20");
    assertThat(optimize(code)).isEqualTo(code);
  }

  @Test
  public void leavesMovBracketReg() {
    ImmutableList<String> code = ImmutableList.of("  mov BYTE [RAX + 1], 0");
    assertThat(optimize(code)).isEqualTo(code);
  }
}
