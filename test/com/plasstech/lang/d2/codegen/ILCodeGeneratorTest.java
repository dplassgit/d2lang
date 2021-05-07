package com.plasstech.lang.d2.codegen;

import org.junit.Test;

import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.StatementsNode;

public class ILCodeGeneratorTest {

  @Test
  public void testGenerate_print() {
    Lexer lexer = new Lexer("print 123");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }

  @Test
  public void testGenerate_assignments() {
    Lexer lexer = new Lexer(
            "a=3 b=-a c=b+4 d=(3-c)/(a*b+9) print c e=true f=!e g=a==b h=(a>b)|(c!=d)&e");

//            "d=(3-4)/(b*b+9)");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
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

    StatementsNode root = (StatementsNode) parser.parse();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }

  @Test
  public void testGenerate_if() {
    Lexer lexer = new Lexer(
            "a=0 if a==0 {print 1} elif ((1 + 2) * (3 - 4) / (-5) == 6) != true "
                    + "{ b=1+2*3-4/5==6!=true|2-3*4-5/-6<7==a & 3+4*5+6/-7>=8%2} "
                    + "else {print 2} print 3");
    Parser parser = new Parser(lexer);

    StatementsNode root = (StatementsNode) parser.parse();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, null);
    codegen.generate();
  }
}