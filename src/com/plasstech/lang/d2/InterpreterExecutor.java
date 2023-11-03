package com.plasstech.lang.d2;

import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.interpreter.Interpreter;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.phase.State;

public class InterpreterExecutor {

  private final CompilationConfiguration config;
  private boolean interactive;
  private State state;
  private InterpreterResult result;
  private int debugInt;

  public InterpreterExecutor(String sourceCode) {
    this(CompilationConfiguration.create(sourceCode));
  }

  public InterpreterExecutor(CompilationConfiguration config) {
    this.config = config;
    this.state = State.create(config.sourceCode()).build();
  }

  public InterpreterExecutor setIntDebugLevel(int debugInt) {
    this.debugInt = debugInt;
    return this;
  }

  public InterpreterResult execute() {
    YetAnotherCompiler yac = new YetAnotherCompiler();
    state = yac.compile(config);
    return execute(state);
  }

  public InterpreterExecutor setInteractive(boolean interactive) {
    this.interactive = interactive;
    return this;
  }

  public InterpreterResult execute(State state) {
    if (state.ilCode() == null || state.ilCode().isEmpty()) {
      throw new IllegalStateException("No il code in state");
    }
    Interpreter interpreter = new Interpreter(state, interactive);
    interpreter.setDebugLevel(debugInt);
    result = interpreter.execute();
    return result;
  }

  public State state() {
    return state;
  }
}
