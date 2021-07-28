package com.plasstech.lang.d2;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.ErrorNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.TypeCheckResult;

public class CodeGenDriver {

  public static void main(String[] args) throws Exception {
    String filename = args[0];
    // 1. read file
    String text = new String(Files.readAllBytes(Paths.get(filename)));
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
    ILCodeGenerator cg = new ILCodeGenerator(root, checkResult.symbolTable());
    System.out.println(Joiner.on("\n").join(cg.generate()));
  }
}
