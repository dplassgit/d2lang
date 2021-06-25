package com.plasstech.lang.d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.lex.Token;

public class LexerDriver {
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
    Token token = lex.nextToken();
    System.out.println(token);
    // 3. output
    while (token.type() != Token.Type.EOF) {
      token = lex.nextToken();
      System.out.println(token);
    }
  }
}
