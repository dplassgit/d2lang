package com.plasstech.lang.d2;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.codegen.RuntimeChecksGenerator;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.optimize.ILOptimizer;
import com.plasstech.lang.d2.optimize.RangeChecker;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;

public class YetAnotherCompiler {

  public State compile(CompilationConfiguration config) {
    State state = State.create(config.sourceCode()).build().addFilename(config.filename());
    Lexer lexer = new Lexer(state.sourceCode());
    Phase parser = new Parser(lexer);
    state = parser.execute(state);
    if (config.parseDebugLevel() > 0) {
      System.out.println("------------------------------");
      System.out.println("\nPARSED PROGRAM:");
      System.out.println(state.programNode());
      System.out.println("------------------------------");
    }
    if (shouldReturn(config, state, PhaseName.PARSE)) {
      return state;
    }

    Phase checker = new StaticChecker();
    state = checker.execute(state);
    if (shouldReturn(config, state, PhaseName.TYPE_CHECK)) {
      return state;
    }

    Phase codegen = new ILCodeGenerator();
    state = codegen.execute(state);
    if (config.codeGenDebugLevel() > 0) {
      System.out.println("------------------------------");
      System.out.println("\nINITIAL INTERMEDIATE CODE:");
      if (state.ilCode() != null) {
        System.out.println(Joiner.on("\n").join(state.ilCode()));
      }
    }
    if (state.error()) {
      if (shouldReturn(config, state, PhaseName.IL_CODEGEN)) {
        return state;
      }
    }

    // Always run the RangeCheckOptimizer even if optimizations are off.
    Phase rangeChecker = new RangeChecker();
    state = rangeChecker.execute(state);
    if (shouldReturn(config, state, PhaseName.IL_CODEGEN)) {
      return state;
    }

    // Run all the optimizers.
    if (config.optimize()) {
      Phase optimizer = new ILOptimizer(config.optDebugLevel());
      state = optimizer.execute(state);
      // throws if it needs to
      if (shouldReturn(config, state, PhaseName.IL_OPTIMIZE)) {
        return state;
      }
      if (config.optimize() && config.optDebugLevel() > 0) {
        System.out.println("------------------------------");
        System.out.println("\nFIRST OPTIMIZED INTERMEDIATE CODE:");
        if (state.lastIlCode() != null) {
          System.out.println(Joiner.on("\n").join(state.lastIlCode()));
        }
        System.out.println("------------------------------");
      }
    }

    if (config.runtimeChecks()) {
      Phase augmented = new RuntimeChecksGenerator();
      state = augmented.execute(state);
      if (config.codeGenDebugLevel() > 0) {
        System.out.println("------------------------------");
        System.out.println("\nAUGMENTED INTERMEDIATE CODE:");
        if (state.ilCode() != null) {
          System.out.println(Joiner.on("\n").join(state.ilCode()));
        }
      }
    }
    if (shouldReturn(config, state, PhaseName.IL_CODEGEN)) {
      return state;
    }

    if (config.optimize()) {
      // Runs all the optimizers.
      Phase optimizer = new ILOptimizer(config.optDebugLevel());
      state = optimizer.execute(state);
      // throws if it needs to
      shouldReturn(config, state, PhaseName.IL_OPTIMIZE);
    }
    if (config.optimize() && config.optDebugLevel() > 0) {
      System.out.println("------------------------------");
      System.out.println("\nFINAL INTERMEDIATE CODE:");
      if (state.lastIlCode() != null) {
        System.out.println(Joiner.on("\n").join(state.lastIlCode()));
      }
      System.out.println("------------------------------");
    }
    return state;
  }

  /** Return true if should return, false if continue. */
  private boolean shouldReturn(CompilationConfiguration config, State state,
      PhaseName currentPhase) {
    // TODO: I hate this.
    if (state.error()) {
      if (config.expectedErrorPhase() != PhaseName.PHASE_UNDEFINED
          && config.expectedErrorPhase() != currentPhase) {
        // error in wrong phase
        System.err.printf("WRONG PHASE: expected %s, was %s\n", config.expectedErrorPhase(),
            currentPhase);
        state.throwOnError();
        return false;
      }
      if (config.expectedErrorMessage() != null
          && !state.errorMessage().matches(config.expectedErrorMessage())) {
        // Wrong error message
        throw new IllegalStateException(
            String.format("WRONG ERROR MESSAGE: expected '%s' to match '%s'",
                state.errorMessage(),
                config.expectedErrorMessage()));
      }

      // error in our phase, good, can stop now.
      return true;
    }
    if (!state.error() && config.expectedErrorPhase() == currentPhase) {
      // bad
      throw new IllegalStateException(
          "Expected error in phase " + config.expectedErrorPhase().name() + " not found");
    }
    // If We're at the right place, stop.
    return currentPhase == config.lastPhase();
  }
}
