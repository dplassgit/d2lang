package com.plasstech.lang.d2.interpreter;

import static com.google.common.truth.Truth.assertThat;

import java.util.List;

import org.junit.Test;

import com.plasstech.lang.d2.codegen.CodeGenerator;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.ProgramNode;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.TypeCheckResult;

public class InterpreterTest {
  @Test
  public void whileLoop() {
    Environment env = execute("i=0 while i < 30 do i=i+1 { print i }");
    assertThat(env.getValue("i")).isEqualTo(30);
  }

  @Test
  public void whileLoopIncrement() {
    Environment env = execute("i=0 while true do i=i+1 { i= i*2 if i > 1000 { break }}");
    assertThat(env.getValue("i")).isEqualTo(1022);
  }

  @Test
  public void simple() {
    Environment env = execute("i=1 j=i");
    assertThat(env.getValue("i")).isEqualTo(1);
    assertThat(env.getValue("j")).isEqualTo(1);
  }

  @Test
  public void simpleIf() {
    Environment env = execute("i=1 j=i if i==1 {i=2 print i } ");
    assertThat(env.getValue("i")).isEqualTo(2);
    assertThat(env.getValue("j")).isEqualTo(1);
    assertThat(env.output()).contains("2");
  }

  @Test
  public void fib() {
    Environment env = execute("n=10\n" //
            + "n1 = 0\n" //
            + "n2 = 1\n" //
            + "i=0 while i < n*2-1 do i = i+1 {\n" //
            + "  if (i%2)==0 {\n" //
            + "    continue\n" //
            + "  }\n" //
            + "  nth = n1 + n2\n" //
            + "  n1 = n2\n" //
            + "  n2 = nth\n" //
            + "  print nth\n" //
            + "}\n" //
            + "");
    assertThat(env.getValue("nth")).isEqualTo(55);
    assertThat(env.getValue("i")).isEqualTo(19);
  }

  @Test
  public void fact() {
    Environment env = execute(
            "          n= 10 fact = 1\n" //
                    + "i=1 while i <= n do i = i+1 {\n" //
                    + "  fact = fact*i\n" //
                    + "  print fact\n" //
                    + "}\n" //
    );

    assertThat(env.getValue("i")).isEqualTo(11);
    assertThat(env.getValue("fact")).isEqualTo(3628800);
  }

  @Test
  public void ifElse() {
    Environment env = execute("n = 0 " //
            + "while n < 10 do n = n + 1 {" //
            + " if n == 1 { print -1 } " //
            + " elif (n == 2) { print -2 } " //
            + " else {" //
            + "   if n==3 {print -3} " //
            + "   else {print n}}" //
            + "}");
    assertThat(env.getValue("n")).isEqualTo(10);
  }

  private Environment execute(String program) {
    Lexer lexer = new Lexer(program);
    Parser parser = new Parser(lexer);
    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    if (result.isError()) {
      throw new RuntimeException(result.message());
    }

    SymTab table = result.symbolTable();

    CodeGenerator<Op> codegen = new ILCodeGenerator(root, table);
    List<Op> operators = codegen.generate();

    Interpreter interpreter = new Interpreter(operators, table);
    Environment env = interpreter.execute();

    System.err.println(env.toString());
    System.err.println(env.output());
    return env;
  }
}
