package com.plasstech.lang.d2;

import java.util.List;

import com.google.common.base.Joiner;
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
  private List<Op> ilCode;
  private Environment env;

  public ExecutionEnvironment(String program) {
    this(program, false);
  }

  public ExecutionEnvironment(String program, boolean optimize) {
    this.program = program;
    this.optimize = optimize;
  }

  public Environment execute() {
    Lexer lexer = new Lexer(program);
    Parser parser = new Parser(lexer);
    Node parseNode = parser.parse();
    if (parseNode.isError()) {
      throw new RuntimeException(parseNode.message());
    }
    programNode = (ProgramNode) parseNode;
    StaticChecker checker = new StaticChecker(programNode);
    typeCheckResult = checker.execute();
    if (typeCheckResult.isError()) {
      throw new RuntimeException(typeCheckResult.message());
    }

    symbolTable = typeCheckResult.symbolTable();

    CodeGenerator<Op> codegen = new ILCodeGenerator(programNode, symbolTable);
    ilCode = codegen.generate();
    if (optimize) {
      ILOptimizer optimizer = new ILOptimizer(ilCode);
      List<Op> optimized = optimizer.optimize();
      if (!optimized.equals(ilCode)) {
        System.out.println("UNOPTIMIZED");
        System.out.println(Joiner.on("\n").join(ilCode));
        System.out.println("\nOPTIMIZED");
        System.out.println(Joiner.on("\n").join(optimized));
        ilCode = optimized;
      }
    }

    return execute(ilCode);
  }

  public Environment execute(List<Op> operators) {
    Interpreter interpreter = new Interpreter(operators, symbolTable);
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

  public List<Op> ilCode() {
    return ilCode;
  }

  public Environment environment() {
    return env;
  }
}
