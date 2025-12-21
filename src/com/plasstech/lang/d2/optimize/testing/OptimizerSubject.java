package com.plasstech.lang.d2.optimize.testing;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.util.Map;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.plasstech.lang.d2.InterpreterExecutor;
import com.plasstech.lang.d2.YetAnotherCompiler;
import com.plasstech.lang.d2.codegen.il.Call;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.OpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.optimize.ILOptimizer;
import com.plasstech.lang.d2.optimize.Optimizer;
import com.plasstech.lang.d2.phase.State;

public class OptimizerSubject extends Subject {
  public static OptimizerSubject assertThatInterpreting(String program) {
    return assertAbout(OptimizerSubject::new).that(program);
  }

  private final String code;
  private Optimizer optimizer = new ILOptimizer(2);

  private OptimizerSubject(FailureMetadata metadata, String code) {
    super(metadata, code);
    this.code = code;
  }

  public OptimizerSubject withOptimizer(Optimizer optimizer) {
    this.optimizer = optimizer;
    return this;
  }

  public void hasNoCalls() {
    InterpreterResult result = optimizeAssertSameVariables();
    long actual = result.code().stream().filter(op -> (op instanceof Call)).count();
    assertThat(actual).isEqualTo(0);
  }

  public void hasCallsTo(String procName) {
    InterpreterResult result = optimizeAssertSameVariables();
    // Show that there are still calls to the procedure
    boolean[] hasCalls = new boolean[1];
    OpcodeVisitor visitor =
        new DefaultOpcodeVisitor() {
          @Override
          public void visit(Call op) {
            hasCalls[0] = true;
            assertThat(op.procSym().name()).isEqualTo(procName);
          }
        };
    for (Op op : result.code()) {
      op.accept(visitor);
    }
    assertThat(hasCalls[0]).isTrue();
  }

  public void hasCompileTimeError(String error) {
    YetAnotherCompiler compiler = new YetAnotherCompiler();
    CompilationConfiguration config =
        CompilationConfiguration.builder().setSourceCode(code).setOptimize(true).build();
    State state = compiler.compile(config);
    assertThat(state.error()).isTrue();
    assertThat(state.errorMessage()).matches(error);
    System.err.printf("Compile time exception: %s\n", state.exception());
  }

  // I don't love returning InterpreterResult
  public InterpreterResult hasSameVariables() {
    return optimizeAssertSameVariables();
  }

  public InterpreterResult hasExpectedSysCallCount(long expected) {
    InterpreterResult result = optimizeAssertSameVariables();
    long actual = result.code().stream().filter(op -> (op instanceof SysCall)).count();
    assertThat(actual).isEqualTo(expected);
    return result;
  }

  private InterpreterResult optimizeAssertSameVariables() {
    InterpreterExecutor ee = new InterpreterExecutor(code);
    InterpreterResult unoptimizedResult = ee.execute();
    ImmutableList<Op> originalCode = unoptimizedResult.code();

    System.out.printf("\nUNOPTIMIZED:\n");
    System.out.println(Joiner.on("\n").join(originalCode));

    System.out.println("\nUNOPTIMIZED SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(unoptimizedResult.environment().output()));
    System.out.println(unoptimizedResult.environment().variables());

    ImmutableList<Op> optimized = optimizer.optimize(originalCode, unoptimizedResult.symbolTable());
    InterpreterResult optimizedResult = ee.execute(ee.state().setIlCode(optimized));

    System.out.printf("\n%s OPTIMIZED:\n", optimizer.getClass().getSimpleName());
    System.out.println(Joiner.on("\n").join(optimized));

    System.out.println("\nOPTIMIZED SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(optimizedResult.environment().output()));
    System.out.println(optimizedResult.environment().variables());

    assertWithMessage("Output should be the same")
        .that(Joiner.on("").join(optimizedResult.environment().output()))
        .isEqualTo(Joiner.on("").join(unoptimizedResult.environment().output()));
    assertMapsSame(
        optimizedResult.environment().variables(), unoptimizedResult.environment().variables());
    // new code should either be faster or smaller or both
    if (unoptimizedResult.linesOfCode() >= optimizedResult.linesOfCode()) {
      assertWithMessage("New code should run in fewer cycles (if it's not smaller)")
          .that(unoptimizedResult.instructionCycles())
          .isAtLeast(optimizedResult.instructionCycles());
    }
    return optimizedResult;
  }

  private static void assertMapsSame(Map<String, Object> actuals, Map<String, Object> expecteds) {
    for (Map.Entry<String, Object> entry : expecteds.entrySet()) {
      // make sure everything's there.
      Object actual = entry.getValue();
      Object expected = actuals.get(entry.getKey());
      assertWithMessage(String.format("Value of %s is wrong", entry.getKey()))
          .that(actual)
          .isEqualTo(expected);
    }
    assertThat(actuals.size()).isAtLeast(expecteds.size());
  }
}
