package com.plasstech.lang.d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.ErrorNode;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.TypeCheckResult;

public class CheckerDriver {

  public static void main(String[] args) {
    String filename = args[0];
    String text;
    try {
      text = new String(Files.readAllBytes(Paths.get(filename)));
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
    Lexer lex = new Lexer(text);
    Parser parser = new Parser(lex);
    Node node = parser.parse();
    if (node.isError()) {
      System.err.println(((ErrorNode) node).message());
      return;
    }
    StaticChecker checker = new StaticChecker((ProgramNode) node);
    TypeCheckResult result = checker.execute();
    System.out.println(node);
    if (result.isError()) {
      System.err.println(result.message());
    }
  }
}
