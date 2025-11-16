package com.plasstech.lang.d2.codegen.x64.optimize;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class AddSubOptimizerTest {
  private Optimizer optimizer = new AddSubOptimizer();

  @Test
  public void addSubRspSame() {
    ImmutableList<String> code = ImmutableList.of("  add RSP, 0x80", "  sub RSP, 0x80");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(output).containsExactly("; add RSP, 0x80", "; sub RSP, 0x80").inOrder();
  }

  @Test
  public void subAddRegistersSame(
      @TestParameter({"RSP", "RAX", "EBX", "CX", "DL"}) String register) {
    ImmutableList<String> code =
        ImmutableList.of(
            String.format("  sub %s, 0x80", register), String.format("  add %s, 0x80", register));

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(output)
        .containsExactly(
            String.format("; sub %s, 0x80", register), String.format("; add %s, 0x80", register))
        .inOrder();
  }

  @Test
  public void addAddRsp() {
    ImmutableList<String> code = ImmutableList.of("  add RSP, 0x10", "  add RSP, 0x08");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(output).containsExactly("; add RSP, 0x10", "  add RSP, 0x18").inOrder();
  }

  @Test
  public void subSubRsp() {
    ImmutableList<String> code = ImmutableList.of("  sub RSP, 0x10", "  sub RSP, 0x08");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(output).containsExactly("; sub RSP, 0x10", "  sub RSP, 0x18").inOrder();
  }

  @Test
  public void addSubRspDifferent() {
    ImmutableList<String> code = ImmutableList.of("  add RSP, 0x80", "  sub RSP, 0x81");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(output).containsExactly("; add RSP, 0x80", "  sub RSP, 0x01").inOrder();
  }

  @Test
  public void addSubDifferentRegisters() {
    ImmutableList<String> code = ImmutableList.of("  add RSP, 0x80", "  sub RAX, 0x81");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isFalse();
    assertThat(output).isEqualTo(code);
  }
}
