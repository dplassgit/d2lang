package com.plasstech.lang.d2.codegen.x64.optimize;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class SingleLinePatternOptimizerTest {
  private SingleLinePatternOptimizer optimizer = new SingleLinePatternOptimizer();

  private ImmutableList<String> optimize(ImmutableList<String> code) {
    while (true) {
      code = optimizer.optimize(code);
      if (!optimizer.isChanged()) {
        return code;
      }
    }
  }

  @Test
  public void replacesAdd1WithInc() {
    ImmutableList<String> code = ImmutableList.of("  add DWORD ECX, 1");
    assertThat(optimize(code)).containsExactly("  inc ECX");
  }

  @Test
  public void replacesAdd1WithInc_comment() {
    ImmutableList<String> code = ImmutableList.of("  add DWORD ECX, 1  ; comment");
    assertThat(optimize(code)).containsExactly("  inc ECX");
  }

  @Test
  public void replacesSub1WithDec() {
    ImmutableList<String> code = ImmutableList.of("  sub BYTE CL, 1");
    assertThat(optimize(code)).containsExactly("  dec CL");
  }

  @Test
  public void removesModifiersFromRegisterInc(
      @TestParameter({"RAX", "EBX", "CX", "DL"}) String register,
      @TestParameter({"inc", "dec"}) String opcode,
      @TestParameter({"BYTE", "WORD ", "DWORD", "QWORD "}) String modifier) {

    ImmutableList<String> code = ImmutableList.of(
        String.format("  %s %s %s", opcode, modifier, register));
    assertThat(optimize(code)).containsExactly(
        String.format("  %s %s", opcode, register));
  }

  @Test
  public void replacesQwordAdd1WithInc() {
    ImmutableList<String> code = ImmutableList.of("  add QWORD [RBP + 28], 1");
    assertThat(optimize(code)).containsExactly("  inc QWORD [RBP + 28]");
  }

  @Test
  public void localAdd1() {
    ImmutableList<String> code = ImmutableList.of("  add BYTE [RBP + 16], 1");
    assertThat(optimize(code)).containsExactly("  inc BYTE [RBP + 16]");
  }

  @Test
  public void replacesMovReg_0_with_xor(
      @TestParameter(
        {"RAX", "EBX", "CX", "DL", "BH", "R0", "R8", "R10", "R15d", "R14b", "RSI", "ESI", "SI",
            "SIL", "RDI", "EDI", "DI", "DIL"}
      ) String register,
      @TestParameter({" ", "BYTE ", "WORD ", "DWORD ", "QWORD "}) String modifier) {

    ImmutableList<String> code =
        ImmutableList.of(String.format("  mov %s%s, 0", modifier, register));
    assertThat(optimize(code)).containsExactly(String.format("  xor %s, %s", register, register));
  }

  @Test
  public void replacesMovReg_0_with_xor_no_modifier(
      @TestParameter(
        {"RAX", "EBX", "CX", "DL", "BH", "R0", "R8", "R10", "R15d", "R14b", "RSI", "ESI", "SI",
            "SIL", "RDI", "EDI", "DI", "DIL"}
      ) String register) {

    ImmutableList<String> code =
        ImmutableList.of(String.format("  mov %s, 0", register));
    assertThat(optimize(code)).containsExactly(String.format("  xor %s, %s", register, register));
  }

  @Test
  public void replacesMovReg_0_with_xorWithFarComment() {
    ImmutableList<String> code = ImmutableList.of("  mov EAX, 0    ;   do it");
    assertThat(optimize(code)).containsExactly(String.format("  xor EAX, EAX"));
  }

  @Test
  public void replacesMovReg_0_with_xorWithNearComment() {
    ImmutableList<String> code = ImmutableList.of("  mov EAX, 0; do it");
    assertThat(optimize(code)).containsExactly(String.format("  xor EAX, EAX"));
  }
}
