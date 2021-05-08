package com.plasstech.lang.d2.codegen;

import org.junit.Test;

import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.ProgramNode;

public class ILCodeGeneratorTest {

  @Test
  public void testGenerate_print() {
    Lexer lexer = new Lexer("print 123");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }

  @Test
  public void testGenerate_assignments() {
    Lexer lexer = new Lexer(
            "a=3 b=-a c=b+4 d=(3-c)/(a*b+9) print c e=true f=!e g=a==b h=(a>b)|(c!=d)&e");

//            "d=(3-4)/(b*b+9)");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }

  @Test
  public void testGenerate_hugeAssignment() {
    Lexer lexer = new Lexer("a=((1 + 2) * (3 - 4) / (-5) == 6) != true\n"
            + " | ((2 - 3) * (4 - 5) / (-6) < 7) == !false & \n"
            + " ((3 + 4) * (5 + 6) / (-7) >= (8 % 2))"
            + "b=1+2*3-4/5==6!=true|2-3*4-5/-6<7==!a & 3+4*5+6/-7>=8%2");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }

  @Test
  public void testGenerate_if() {
    Lexer lexer = new Lexer(
            "a=0 if a==0 {print 1} elif ((-5) == 6) != true { b=1+2*3} "
                    + "else {print 2} print 3");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }

  @Test
  public void testGenerate_main() {
    Lexer lexer = new Lexer("a=0 main {print a}");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    System.err.println(root);
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }

  @Test
  public void testGenerate_while() {
    Lexer lexer = new Lexer("i=0 while i < 30 do i = i+1 {print i}");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    System.err.println(root);
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }

  @Test
  public void testGenerate_whileContinue() {
    Lexer lexer = new Lexer("i=0 while i < 30 do i = i+1 {if i > 10 { continue } print i} print 1");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    System.err.println(root);
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }

  @Test
  public void testGenerate_whileBreak() {
    Lexer lexer = new Lexer("i=0 while i < 30 do i = i+1 {if i > 10  { break } print i} print -1");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    System.err.println(root);
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }
}