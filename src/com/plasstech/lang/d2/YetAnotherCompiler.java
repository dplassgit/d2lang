package com.plasstech.lang.d2;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.optimize.ILOptimizer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;

public class YetAnotherCompiler {

  public State compile(CompilationConfiguration config) {
    State state = State.create(config.sourceCode()).build().addFilename(config.filename());
    Lexer lexer = new Lexer(state.sourceCode());
    Parser parser = new Parser(lexer);
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

    StaticChecker checker = new StaticChecker();
    state = checker.execute(state);
    if (shouldReturn(config, state, PhaseName.TYPE_CHECK)) {
      return state;
    }

    ILCodeGenerator codegen = new ILCodeGenerator();
    state = codegen.execute(state);
    boolean shouldReturn = shouldReturn(config, state, PhaseName.IL_CODEGEN);
    if (config.codeGenDebugLevel() > 0) {
      System.out.println("------------------------------");
      System.out.println("\nINITIAL INTERMEDIATE CODE:");
      if (state.ilCode() != null) {
        System.out.println(Joiner.on("\n").join(state.ilCode()));
      }
      System.out.println("------------------------------");
    }
    if (shouldReturn) {
      return state;
    }

    if (config.optimize()) {
      // Runs all the optimizers.
      ILOptimizer optimizer = new ILOptimizer(config.optDebugLevel());
      state = optimizer.execute(state);
      if (state.optimizedIlCode() != null) {
        if (config.codeGenDebugLevel() > 0) {
          System.out.println("------------------------------");
          System.out.println("\nFINAL INTERMEDIATE CODE:");
          System.out.println(Joiner.on("\n").join(state.optimizedIlCode()));
          System.out.println("------------------------------");
        }
      }
      // throws if it needs to
      shouldReturn(config, state, PhaseName.IL_OPTIMIZE);
    }
    return state;
  }

  /** Return true if should return, false if continue. */
  private boolean shouldReturn(CompilationConfiguration config, State state,
      PhaseName currentPhase) {
    if (state.error()) {
      if (config.expectedErrorPhase() != currentPhase) {
        // error in wrong phase
        System.err.printf("WRONG PHASE: expected %s, was %s\n", config.expectedErrorPhase(),
            currentPhase);
        state.throwOnError();
        return false;
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
