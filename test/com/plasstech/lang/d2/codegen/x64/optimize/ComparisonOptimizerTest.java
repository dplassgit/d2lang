package com.plasstech.lang.d2.codegen.x64.optimize;

import static com.google.common.truth.Truth.assertThat;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(TestParameterInjector.class)
public class ComparisonOptimizerTest {
  private ComparisonOptimizer optimizer = new ComparisonOptimizer();

  @Test
  public void optimizes(
      @TestParameter({"AL", "BL", "CL", "DL", "SIL", "DIL"}) String register,
      @TestParameter({"z", "g", "ge"}) String flag,
      @TestParameter({"0", "1", "EAX"}) String target) {

    ImmutableList<String> code =
        ImmutableList.of(
            String.format("  cmp DWORD [_NUM_PLANETS], %s ; direct comparison", target),
            String.format("  set%s %s", flag, register),
            String.format("  cmp BYTE %s, 0", register),
            "  je __loop_end_75");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(output)
        .containsExactly(
            code.get(0),
            ";" + code.get(1),
            ";" + code.get(2),
            String.format("  j%s __loop_end_75", ComparisonOptimizer.toOpposite(flag)));
  }

  @Test
  public void optimizesJne(
      @TestParameter({"R8b", "R9b", "R10b", "R11b", "R12b", "R13b", "R14b", "R15b"})
          String register,
      @TestParameter({"nz", "l", "le"}) String flag) {

    ImmutableList<String> code =
        ImmutableList.of(
            "  cmp DWORD [_NUM_PLANETS], 0 ; direct comparison",
            String.format("  set%s %s", flag, register),
            String.format("  cmp BYTE %s, 0", register),
            "  jne __loop_end_75");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isTrue();
    assertThat(output)
        .containsExactly(
            code.get(0),
            ";" + code.get(1),
            ";" + code.get(2),
            String.format("  j%s __loop_end_75", flag));
  }

  @Test
  public void noOptimizationIfNotSequential() {
    ImmutableList<String> code =
        ImmutableList.of(
            "  cmp DWORD [_NUM_PLANETS], 0  ; direct comparison",
            "  setg BL",
            "  something in between",
            "  cmp BYTE BL, 0",
            "  je __loop_end_75");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isFalse();
    assertThat(output).isEqualTo(code);
  }

  @Test
  public void noOptimizationIfNotComplete() {
    ImmutableList<String> code =
        ImmutableList.of(
            "  cmp DWORD [_NUM_PLANETS], 0  ; direct comparison", "  setg BL", "  cmp BYTE BL, 0");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isFalse();
    assertThat(output).isEqualTo(code);
  }

  @Test
  public void noOptimizationIfWrongRegister() {
    ImmutableList<String> code =
        ImmutableList.of(
            "  cmp DWORD [_NUM_PLANETS], 0  ; direct comparison",
            "  setg BL",
            "  cmp BYTE CL, 0",
            "  je __loop_end_75");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isFalse();
    assertThat(output).isEqualTo(code);
  }

  @Test
  public void noOptimizationIfWrongSize() {
    ImmutableList<String> code =
        ImmutableList.of(
            "  cmp DWORD [_NUM_PLANETS], 0  ; direct comparison",
            "  setg BL",
            "  cmp DWORD BX, 0",
            "  je __loop_end_75");

    List<String> output = optimizer.doOptimize(code);
    assertThat(optimizer.isChanged()).isFalse();
    assertThat(output).isEqualTo(code);
  }
}
