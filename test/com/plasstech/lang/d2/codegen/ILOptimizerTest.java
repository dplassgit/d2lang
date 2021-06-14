package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.ExecutionEnvironment;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.Environment;

public class ILOptimizerTest {

  @Test
  public void plusZero() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 0 + 1 b = a + 0 c = 2 + 0");
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));
    assertThat(env.getValue("a")).isEqualTo(1);
    assertThat(env.getValue("b")).isEqualTo(1);
    assertThat(env.getValue("c")).isEqualTo(2);

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    env = ee.execute(optimized);
    assertThat(env.getValue("a")).isEqualTo(1);
    assertThat(env.getValue("b")).isEqualTo(1);
    assertThat(env.getValue("c")).isEqualTo(2);
  }

  @Test
  public void plusConstants() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 2 + 3");
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));
    assertThat(env.getValue("a")).isEqualTo(5);

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    env = ee.execute(optimized);
    assertThat(env.getValue("a")).isEqualTo(5);
  }

  @Test
  public void minusConstants() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 2 - 3");
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));
    assertThat(env.getValue("a")).isEqualTo(-1);

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    env = ee.execute(optimized);
    assertThat(env.getValue("a")).isEqualTo(-1);
  }

  @Test
  public void divConstants() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 10 / 2");
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));
    assertThat(env.getValue("a")).isEqualTo(5);

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    env = ee.execute(optimized);
    assertThat(env.getValue("a")).isEqualTo(5);
  }

  @Test
  public void divByZero() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 1 / 0");
    try {
      ee.execute();
    } catch (Exception e) {
    }
    System.out.println(Joiner.on("\n").join(ee.ilCode()));

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    try {
      ee.execute(optimized);
    } catch (Exception e) {
    }
  }

  @Test
  public void modConstants() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 14 % 5");
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));
    assertThat(env.getValue("a")).isEqualTo(4);

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    env = ee.execute(optimized);
    assertThat(env.getValue("a")).isEqualTo(4);
  }

  @Test
  public void plusStringConstants() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 'hi' + ' there'");
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));
    assertThat(env.getValue("a")).isEqualTo("hi there");

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    env = ee.execute(optimized);
    assertThat(env.getValue("a")).isEqualTo("hi there");
  }

  @Test
  public void timesZero() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 1 * 2 b = a * 0 d = 3 * 0");
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));
    assertThat(env.getValue("a")).isEqualTo(2);
    assertThat(env.getValue("b")).isEqualTo(0);
    assertThat(env.getValue("d")).isEqualTo(0);

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    env = ee.execute(optimized);
    assertThat(env.getValue("a")).isEqualTo(2);
    assertThat(env.getValue("b")).isEqualTo(0);
    assertThat(env.getValue("d")).isEqualTo(0);
  }

  @Test
  public void timesConstants() {
    ExecutionEnvironment ee = new ExecutionEnvironment("a = 2 * 3");
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));
    assertThat(env.getValue("a")).isEqualTo(6);

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    env = ee.execute(optimized);
    assertThat(env.getValue("a")).isEqualTo(6);
  }

  @Test
  public void arrayConstants() {
    ExecutionEnvironment ee =
        new ExecutionEnvironment("a=[2,4,6] i=0 while i < 3 do i = i + 1 { print a[i] }");
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));

    List<Op> optimized = new ILOptimizer(ee.ilCode()).optimize();
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    env = ee.execute(optimized);
  }
}
