package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.ExecutionEnvironment;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.Environment;

public class TestUtils {

  static void optimizeAssertSameVariables(String program) {
    ExecutionEnvironment ee = new ExecutionEnvironment(program);
    Environment env = ee.execute();
    System.out.println(Joiner.on("\n").join(ee.ilCode()));
  
    List<Op> originalCode = new ArrayList<>(ee.ilCode());
  
    List<Op> optimized = new ILOptimizer().optimize(ee.ilCode());
    System.out.println("\nOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(optimized));
    Environment env2 = ee.execute(optimized);
    assertThat(env2.variables()).isEqualTo(env.variables());
    assertThat(env2.output()).isEqualTo(env.output());
  
    assertWithMessage("Should have made at least one optimization")
        .that(originalCode)
        .isNotEqualTo(optimized);
  }}
