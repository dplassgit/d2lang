package com.plasstech.lang.d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.plasstech.lang.d2.codegen.PseudoAsmCodeGenerator;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.ErrorNode;
import com.plasstech.lang.d2.parse.Node;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.ProgramNode;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.TypeCheckResult;

public class PseudoAsmCodeGenDriver {

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
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult checkResult = checker.execute();
    if (checkResult.isError()) {
      throw new RuntimeException(checkResult.message());
    }
    System.out.println(root);
    PseudoAsmCodeGenerator cg = new PseudoAsmCodeGenerator(root);
    cg.generate();
  }
}
