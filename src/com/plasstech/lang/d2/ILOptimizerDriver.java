package com.plasstech.lang.d2;

import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.optimize.ILOptimizer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;

public class ILOptimizerDriver {

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
    ImmutableList<Op> unoptimizedCode = state.ilCode();
    System.out.println("UNOPTIMIZED:");
    System.out.println(Joiner.on("\n").join(unoptimizedCode));
    System.out.println();
    
    ILOptimizer optimizer = new ILOptimizer(2);
    state = optimizer.execute(state);
    System.out.println("OPTIMIZED:");
    System.out.println(Joiner.on("\n").join(state.optimizedIlCode()));
  }
}
