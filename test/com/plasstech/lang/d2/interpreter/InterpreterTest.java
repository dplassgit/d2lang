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
    Lexer lexer = new Lexer("i=0 while i < 30 do i=i+1 { print i }");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    SymTab table = result.symbolTable();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, table);
    List<Op> operators = codegen.generate();
//    for (int i = 0; i < operators.size(); ++i) {
//      System.out.printf("%d: %s\n", i + 1, operators.get(i));
//    }
    Interpreter interpreter = new Interpreter(operators, table);
    Environment env = interpreter.execute();

    System.err.println(env.output());
    assertThat(env.getValue("i")).isEqualTo(30);
  }

  @Test
  public void simple() {
    Lexer lexer = new Lexer("i=1 j=i");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    SymTab table = result.symbolTable();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, table);
    List<Op> operators = codegen.generate();
    Interpreter interpreter = new Interpreter(operators, table);
    Environment env = interpreter.execute();

    assertThat(env.getValue("i")).isEqualTo(1);
    assertThat(env.getValue("j")).isEqualTo(1);
  }

  @Test
  public void simpleIf() {
    Lexer lexer = new Lexer("i=1 j=i if i==1 {i=2 print i } ");
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    SymTab table = result.symbolTable();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, table);
    List<Op> operators = codegen.generate();
    Interpreter interpreter = new Interpreter(operators, table);
    Environment env = interpreter.execute();

    assertThat(env.getValue("i")).isEqualTo(2);
    assertThat(env.getValue("j")).isEqualTo(1);
    assertThat(env.output()).contains("2");
  }

  @Test
  public void fib() {
    Lexer lexer = new Lexer("n=10\n" //
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
    Parser parser = new Parser(lexer);

    ProgramNode root = (ProgramNode) parser.parse();
    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    SymTab table = result.symbolTable();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, table);
    List<Op> operators = codegen.generate();
    Interpreter interpreter = new Interpreter(operators, table);
    Environment env = interpreter.execute();

    System.err.println(env.toString());
    System.err.println(env.output());
    assertThat(env.getValue("nth")).isEqualTo(55);
    assertThat(env.getValue("i")).isEqualTo(19);
  }
  
}
