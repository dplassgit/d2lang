package com.plasstech.lang.d2;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.interpreter.ExecutionResult;
import com.plasstech.lang.d2.interpreter.Interpreter;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.optimize.ILOptimizer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.SymTab;

/** TODO: Rename this "Executor" */
@SuppressWarnings("unused")
public class ExecutionEnvironment {

  private boolean optimize;
  private boolean interactive;
  private int debuglex;
  private int debugOpt;
  private int debugCodeGen;
  private int debugType;
  private int debugParse;
  private int debugInt;
  private State state;
  private ExecutionResult result;

  /** TODO: Make this a builder */
  public ExecutionEnvironment(String sourceCode) {
    this.state = State.create(sourceCode).build();
  }

  public ExecutionEnvironment setOptimize(boolean optimize) {
    this.optimize = optimize;
    return this;
  }

  public ExecutionEnvironment setInteractive(boolean interactive) {
    this.interactive = interactive;
    return this;
  }

  public ExecutionEnvironment setLexDebugLevel(int level) {
    this.debuglex = level;
    return this;
  }

  public ExecutionEnvironment setParseDebugLevel(int level) {
    this.debugParse = level;
    return this;
  }

  public ExecutionEnvironment setTypeDebugLevel(int level) {
    this.debugType = level;
    return this;
  }

  public ExecutionEnvironment setCodeGenDebugLevel(int level) {
    this.debugCodeGen = level;
    return this;
  }

  public ExecutionEnvironment setOptDebugLevel(int level) {
    this.debugOpt = level;
    return this;
  }

  public ExecutionEnvironment setIntDebugLevel(int level) {
    this.debugInt = level;
    return this;
  }

  public ExecutionResult execute() {
    Lexer lexer = new Lexer(state.sourceCode());
    Parser parser = new Parser(lexer);
    state = parser.execute(state);
    if (state.error()) {
      throw state.exception();
    }
    ProgramNode programNode = state.programNode();
    state = state.addProgramNode(programNode);
    StaticChecker checker = new StaticChecker();
    state = checker.execute(state);
    if (state.error()) {
      throw state.exception();
    }

    SymTab symbolTable = state.symbolTable();

    ILCodeGenerator codegen = new ILCodeGenerator();
    state = codegen.execute(state);
    if (debugCodeGen > 0) {
      System.out.println("\nUNOPTIMIZED:");
      System.out.println("------------------------------");
      System.out.println(Joiner.on("\n").join(state.ilCode()));
    }
    if (optimize) {
      // Runs all the optimizers.
      ILOptimizer optimizer = new ILOptimizer(debugOpt);
      state = optimizer.execute(state);
      if (debugOpt > 0) {
        System.out.println("\nOPTIMIZED:");
        System.out.println("------------------------------");
        System.out.println(Joiner.on("\n").join(state.optimizedIlCode()));
        System.out.println();
      }
    }

    return execute(state);
  }

  public ExecutionResult execute(State state) {
    Interpreter interpreter = new Interpreter(state, interactive);
    interpreter.setDebugLevel(debugInt);
    result = interpreter.execute();
    return result;
  }

  public State state() {
    return state;
  }
}
