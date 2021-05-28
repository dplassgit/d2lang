package com.plasstech.lang.d2.codegen;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.ProgramNode;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.TypeCheckResult;

public class ILCodeGeneratorTest {

  @Test
  public void generate_print() {
    generateProgram("print 123");
  }

  @Test
  public void generate_assignments() {
    generateProgram(
            "a=3 b=-a c=b+4 d=(3-c)/(a*b+9) print c e=true f=!e g=a==b h=(a>b)|(c!=d)&e");
  }

  @Test
  public void generate_stringAssignment() {
    generateProgram("a:string a='hi' print a");
  }

  @Test
  public void generate_println() {
    generateProgram("a='world' print 'hello, ' println a");
  }

  @Test
  public void generate_stringExpression() {
    generateProgram("a='hi' b=a+' world'");
  }

  @Test
  public void generate_hugeAssignment() {
    generateProgram("a=((1 + 2) * (3 - 4) / (-5) == 6) != true\n"
            + " | ((2 - 3) * (4 - 5) / (-6) < 7) == !false & \n"
            + " ((3 + 4) * (5 + 6) / (-7) >= (8 % 2))"
            + "b=1+2*3-4/5==6!=true|2-3*4-5/-6<7==!a & 3+4*5+6/-7>=8%2");
  }

  @Test
  public void generate_if() {
    generateProgram("a=0 if a==0 {print 1} elif ((-5) == 6) != true { b=1+2*3} "
                    + "else {print 2} print 3");
  }

  @Test
  public void generate_main() {
    generateProgram("a=0 main {print a}");
  }

  @Test
  public void generate_while() {
    generateProgram("i=0 while i < 30 do i = i+1 {print i}");
  }

  @Test
  public void generate_whileContinue() {
    generateProgram("i=0 while i < 30 do i = i+1 {if i > 10 { continue } print i} print 1");
  }

  @Test
  public void generate_whileBreak() {
    generateProgram("i=0 while i < 30 do i = i+1 {if i > 10  { break } print i} print -1");
  }

  @Test
  public void generate_whileNestedBreak() {
    generateProgram(
            "i=0 while i < 30 do i = i+1 { "
                    + "  j = 0 while j < 10 do j = j + 1 { " //
                    + "    print j break" //
                    + "  }" //
                    + "   if i > 10  { break } print i" //
                    + "}" //
                    + "print -1");
  }

  private List<Op> generateProgram(String program) {
    Lexer lexer = new Lexer(program);
    Parser parser = new Parser(lexer);
    ProgramNode root = (ProgramNode) parser.parse();
    System.out.printf("// %s\n", root.toString());

    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    if (result.isError()) {
      fail(result.message());
    }
    SymTab table = result.symbolTable();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, table);
    return codegen.generate();
  }
}