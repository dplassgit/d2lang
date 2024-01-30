package com.plasstech.lang.d2.codegen.t100;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class DoublesTest {
  @Test
  public void zero() {
    double input = 0;
    int[] output = Doubles.toT100Format(input);
    assertThat(output).hasLength(8);
    assertThat(output).asList().containsExactly(0, 0, 0, 0, 0, 0, 0, 0).inOrder();
  }

  @Test
  public void big() {
    double input = 1234567.8765432;
    int[] output = Doubles.toT100Format(input);
    assertThat(output).hasLength(8);
    assertThat(output).asList().containsExactly(71, 18, 52, 86, 120, 118, 84, 50).inOrder();
  }

  @Test
  public void negative_big() {
    double input = -2345.678;
    int[] output = Doubles.toT100Format(input);
    assertThat(output).hasLength(8);
    assertThat(output).asList().containsExactly(196, 35, 69, 103, 128, 0, 0, 0).inOrder();
  }

  @Test
  public void one() {
    double input = 1;
    int[] output = Doubles.toT100Format(input);
    assertThat(output).hasLength(8);
    assertThat(output).asList().containsExactly(65, 16, 0, 0, 0, 0, 0, 0).inOrder();
  }

  @Test
  public void negative_one() {
    double input = -1;
    int[] output = Doubles.toT100Format(input);
    assertThat(output).hasLength(8);
    assertThat(output).asList().containsExactly(193, 16, 0, 0, 0, 0, 0, 0).inOrder();
  }

  @Test
  public void one_point() {
    double input = 1.2345678765432;
    int[] output = Doubles.toT100Format(input);
    assertThat(output).hasLength(8);
    assertThat(output).asList().containsExactly(65, 18, 52, 86, 120, 118, 84, 50).inOrder();
  }

  @Test
  public void small() {
    double input = 0.000012345678765432;
    int[] output = Doubles.toT100Format(input);
    assertThat(output).hasLength(8);
    assertThat(output).asList().containsExactly(60, 18, 52, 86, 120, 118, 84, 50).inOrder();
  }

  @Test
  public void negative_small() {
    double input = -.002345;
    int[] output = Doubles.toT100Format(input);
    assertThat(output).hasLength(8);
    assertThat(output).asList().containsExactly(190, 35, 69, 0, 0, 0, 0, 0).inOrder();
  }

}
