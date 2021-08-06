package com.plasstech.lang.d2;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;

public class CodeGenDriver {

  public static void main(String[] args) throws Exception {
    String filename = args[0];
    // 1. read file
    String text = new String(Files.readAllBytes(Paths.get(filename)));
    // 2. lex
    Lexer lex = new Lexer(text);
    Parser parser = new Parser(lex);
    State state = parser.execute(State.create(text).build());
    if (state.error()) {
      throw state.exception();
    }
    StaticChecker checker = new StaticChecker();
    state = checker.execute(state);
    if (state.error()) {
      throw state.exception();
    }
    ILCodeGenerator cg = new ILCodeGenerator();
    state = cg.execute(state);
    ImmutableList<Op> code = state.ilCode();
    System.out.println(Joiner.on("\n").join(code));
  }
}
