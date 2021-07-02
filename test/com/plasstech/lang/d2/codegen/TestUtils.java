package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertWithMessage;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.ExecutionEnvironment;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.Environment;

public class TestUtils {

  static void optimizeAssertSameVariables(String program) {
    optimizeAssertSameVariables(program, new ILOptimizer());
  }

  static void optimizeAssertSameVariables(String program, Optimizer optimizer) {
    ExecutionEnvironment ee = new ExecutionEnvironment(program);
    Environment unoptimizedEnv = ee.execute();
    //    System.out.printf("\nUNOPTIMIZED:\n");
    //    System.out.println(Joiner.on("\n").join(ee.ilCode()));
    //
    //    System.out.println("\nUNOPTIMIZED SYSTEM.OUT:");
    //    System.out.println("------------------------------");
    //    System.out.println(Joiner.on("").join(unoptimizedEnv.output()));

    List<Op> originalCode = ImmutableList.copyOf(ee.ilCode());

    List<Op> optimized = optimizer.optimize(ee.ilCode());
    Environment optimizedEnv = ee.execute(optimized);

    System.out.printf("\n%s OPTIMIZED:\n", optimizer.getClass().getSimpleName());
    System.out.println(Joiner.on("\n").join(optimized));

    System.out.println("\nOPTIMIZED SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(optimizedEnv.output()));

    assertWithMessage("Environment should be the same")
        .that(optimizedEnv.variables())
        .isEqualTo(unoptimizedEnv.variables());
    assertWithMessage("Output should be the same")
        .that(optimizedEnv.output())
        .isEqualTo(unoptimizedEnv.output());
    assertWithMessage("Should have made at least one optimization")
        .that(originalCode)
        .isNotEqualTo(optimized);
  }
}
