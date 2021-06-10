package com.plasstech.lang.d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.codegen.CodeGenerator;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.Environment;
import com.plasstech.lang.d2.interpreter.Interpreter;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.ErrorNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.TypeCheckResult;

public class InterpreterDriver {

  public static void main(String[] args) {
    String filename = args[0];
    // 1. read file
    String text;
    try {
      text = new String(Files.readAllBytes(Paths.get(filename)));
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    // 2. lex
    Lexer lex = new Lexer(text);
    Parser parser = new Parser(lex);
    Node node = parser.parse();
    if (node.isError()) {
      throw new RuntimeException(((ErrorNode) node).message());
    }
    ProgramNode root = (ProgramNode) node;
    System.out.println("\nPARSED PROGRAM:");
    System.out.println(root);

    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult checkResult = checker.execute();
    if (checkResult.isError()) {
      throw new RuntimeException(checkResult.message());
    }
    System.out.println("\nTYPE-CHECKED PROGRAM:");
    System.out.println(root);

    System.out.println("\nSYMBOL TABLE:");
    System.out.println(checkResult.symbolTable());

    CodeGenerator<Op> cg = new ILCodeGenerator(root, checkResult.symbolTable());
    List<Op> opcodes = cg.generate();
    Interpreter interpreter = new Interpreter(opcodes, checkResult.symbolTable());

    Environment env = interpreter.execute();

    System.out.println("------------------------------");
    System.out.println("SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(env.output()));
  }
}
