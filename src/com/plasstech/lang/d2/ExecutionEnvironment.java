package com.plasstech.lang.d2;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.CodeGenerator;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.ExecutionResult;
import com.plasstech.lang.d2.interpreter.Interpreter;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.optimize.ILOptimizer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.TypeCheckResult;

@SuppressWarnings("unused")
public class ExecutionEnvironment {

  private final String program;
  private boolean optimize;
  private boolean interactive;
  private int debuglex;
  private int debugOpt;
  private int debugCodeGen;
  private int debugType;
  private int debugParse;
  private int debugInt;
  private ProgramNode programNode;
  private TypeCheckResult typeCheckResult;
  private SymTab symbolTable;
  private ImmutableList<Op> ilCode;
  private ExecutionResult result;

  /** TODO: Make this a builder */
  public ExecutionEnvironment(String program) {
    this.program = program;
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
    Lexer lexer = new Lexer(program);
    Parser parser = new Parser(lexer);
    Node parseNode = parser.parse();
    if (parseNode.isError()) {
      // TODO: Throw the ParseException instead.
      throw new RuntimeException(parseNode.message());
    }
    programNode = (ProgramNode) parseNode;
    StaticChecker checker = new StaticChecker(programNode);
    typeCheckResult = checker.execute();
    if (typeCheckResult.isError()) {
      // TODO: Throw the TypeException instead.
      throw new RuntimeException(typeCheckResult.message());
    }

    symbolTable = typeCheckResult.symbolTable();

    CodeGenerator<Op> codegen = new ILCodeGenerator(programNode, symbolTable);
    ilCode = codegen.generate();
    if (debugCodeGen > 0) {
      System.out.println("\nUNOPTIMIZED:");
      System.out.println("------------------------------");
      System.out.println(Joiner.on("\n").join(ilCode));
    }
    if (optimize) {
      // Runs all the optimizers.
      ILOptimizer optimizer = new ILOptimizer(debugOpt);
      ilCode = optimizer.optimize(ilCode);
      if (debugOpt > 0) {
        System.out.println("\nOPTIMIZED:");
        System.out.println("------------------------------");
        System.out.println(Joiner.on("\n").join(ilCode));
        System.out.println();
      }
    }

    return execute(ilCode);
  }

  public ExecutionResult execute(ImmutableList<Op> code) {
    Interpreter interpreter = new Interpreter(code, symbolTable, interactive);
    interpreter.setDebugLevel(debugInt);
    result = interpreter.execute();
    return result;
  }

  public ProgramNode programNode() {
    return programNode;
  }

  public ImmutableList<Op> code() {
    return ilCode;
  }
}
