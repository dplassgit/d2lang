package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertWithMessage;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.ExecutionEnvironment;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.ExecutionResult;

public class TestUtils {

  static ExecutionResult optimizeAssertSameVariables(String program) {
    return optimizeAssertSameVariables(program, new ILOptimizer(2));
  }

  static ExecutionResult optimizeAssertSameVariables(String program, Optimizer optimizer) {
    ExecutionEnvironment ee = new ExecutionEnvironment(program);
    ExecutionResult unoptimizedResult = ee.execute();
    System.out.printf("\nUNOPTIMIZED:\n");
    System.out.println(Joiner.on("\n").join(unoptimizedResult.code()));

    System.out.println("\nUNOPTIMIZED SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(unoptimizedResult.environment().output()));

    ImmutableList<Op> originalCode = ImmutableList.copyOf(unoptimizedResult.code());

    ImmutableList<Op> optimized = optimizer.optimize(originalCode);
    System.out.printf("\n%s OPTIMIZED:\n", optimizer.getClass().getSimpleName());
    System.out.println(Joiner.on("\n").join(optimized));

    ExecutionResult optimizedResult = ee.execute(optimized);

    System.out.println("\nOPTIMIZED SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(optimizedResult.environment().output()));

    assertWithMessage("Environment should be the same")
        .that(optimizedResult.environment().variables())
        .isEqualTo(unoptimizedResult.environment().variables());
    assertWithMessage("Output should be the same")
        .that(optimizedResult.environment().output())
        .isEqualTo(unoptimizedResult.environment().output());
    //    assertWithMessage("New code should be smaller")
    //        .that(unoptimizedResult.linesOfCode())
    //        .isAtLeast(optimizedResult.linesOfCode());
    assertWithMessage("New code should run in fewer cycles")
        .that(unoptimizedResult.instructionCycles())
        .isAtLeast(optimizedResult.instructionCycles());
    return optimizedResult;
  }
}
