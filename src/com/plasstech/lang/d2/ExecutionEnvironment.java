package com.plasstech.lang.d2;

import java.util.List;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.CodeGenerator;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.codegen.ILOptimizer;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.Environment;
import com.plasstech.lang.d2.interpreter.Interpreter;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.TypeCheckResult;

public class ExecutionEnvironment {

  private final String program;
  private final boolean optimize;
  private ProgramNode programNode;
  private TypeCheckResult typeCheckResult;
  private SymTab symbolTable;
  private ImmutableList<Op> ilCode;
  private Environment env;
  private int debuglex;
  private int debugOpt;
  private int debugCodeGen;
  private int debugType;
  private int debugParse;
  private int debugInt;

  public ExecutionEnvironment(String program) {
    this(program, false);
  }

  public ExecutionEnvironment(String program, boolean optimize) {
    this.program = program;
    this.optimize = optimize;
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

  public Environment execute() {
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
    }

    return execute(ilCode);
  }

  public Environment execute(List<Op> operators) {
    Interpreter interpreter = new Interpreter(operators, symbolTable);
    interpreter.setDebugLevel(debugInt);
    env = interpreter.execute();
    return env;
  }

  public ProgramNode programNode() {
    return programNode;
  }

  public TypeCheckResult typeCheckResult() {
    return typeCheckResult;
  }

  public SymTab symbolTable() {
    return symbolTable;
  }

  public ImmutableList<Op> ilCode() {
    return ilCode;
  }

  public Environment environment() {
    return env;
  }
}
