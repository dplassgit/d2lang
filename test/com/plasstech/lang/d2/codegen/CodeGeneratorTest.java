package com.plasstech.lang.d2.codegen;

import org.junit.Test;

import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.StatementsNode;

public class CodeGeneratorTest {

  @Test
  public void testGenerate_print() {
    Lexer lexer = new Lexer("print 123");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    CodeGenerator codegen = new CodeGenerator(root);
    codegen.generate();
  }

  @Test
  public void testGenerate_assignments() {
    Lexer lexer = new Lexer(
            "a=3 b=-a c=b+4 d=(3-c)/(a*b+9) print c e=true f=!e g=a==b h=(a>b)|(c!=d)&e");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    CodeGenerator codegen = new CodeGenerator(root);
    codegen.generate();
  }
}
