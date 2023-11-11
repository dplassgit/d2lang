package com.plasstech.lang.d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.lex.Token;

/**
 * To run:
 * 
 * <pre>
 * bazel run src/com/plasstech/lang/d2:LexerDriver -- $PWD/samples/helloworld.d
 * </pre>
 */
public class LexerDriver {
  public static void main(String[] args) {
    try {
      String filename = args[0];
      // 1. read file
      String text = new String(Files.readAllBytes(Paths.get(filename)));
      // 2. lex
      Lexer lex = new Lexer(text);
      Token token = lex.nextToken();
      System.out.println(token);
      // 3. output
      while (token.type() != TokenType.EOF) {
        token = lex.nextToken();
        System.out.println(token);
      }
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }
}
