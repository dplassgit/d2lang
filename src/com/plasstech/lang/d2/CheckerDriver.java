package com.plasstech.lang.d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;

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
    State state = parser.execute(State.create(text).build());
    if (state.error()) {
      System.err.println(state.errorMessage());
      return;
    }
    ProgramNode node = state.programNode();
    System.out.println("BEFORE:");
    System.out.println(node);
    StaticChecker checker = new StaticChecker();
    state = checker.execute(state);
    System.out.println("\nAFTER:");
    System.out.println(node);
    if (state.error()) {
      System.err.println(state.exception().getMessage());
    }
  }
}
