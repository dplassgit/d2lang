package com.plasstech.lang.d2.codegen;

import static org.junit.Assert.fail;

import java.util.List;

import org.junit.Test;

import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.TypeCheckResult;

public class ILCodeGeneratorTest {

  @Test
  public void print() {
    generateProgram("print 123");
  }

  @Test
  public void simpleIf() {
    generateProgram("i=1 j=i if 1==i {i=2 print i } ");
  }

  @Test
  public void assignments() {
    generateProgram(
        "a=3 b=-a c=b+4 d=(3-c)/(a*b+9) print c e=true f=not e g=a==b h=(a>b) or (c!=d) and e");
  }

  @Test
  public void stringAssignment() {
    generateProgram("a:string a='hi' print a");
  }

  @Test
  public void println() {
    generateProgram("a='world' print 'hello, ' println a");
  }

  @Test
  public void stringExpression() {
    generateProgram("a='hi' b=a+' world'");
  }

  @Test
  public void hugeAssignment() {
    generateProgram(
        "a=((1 + 2) * (3 - 4) / (-5) == 6) != true\n"
            + " or ((2 - 3) * (4 - 5) / (-6) < 7) == not false and \n"
            + " ((3 + 4) * (5 + 6) / (-7) >= (8 % 2))"
            + "b=1+2*3-4/5==6!=true or 2-3*4-5/-6<7==not a and 3+4*5+6/-7>=8%2");
  }

  @Test
  public void ifStmt() {
    generateProgram(
        "a=0 if a==0 {print 1} elif ((-5) == 6) != true { b=1+2*3} else {print 2} print 3");
  }

  @Test
  public void main() {
    generateProgram("a=0 main {print a}");
  }

  @Test
  public void whileStmt() {
    generateProgram("i=0 while i < 30 do i = i+1 {print i}");
  }

  @Test
  public void whileContinue() {
    generateProgram("i=0 while i < 30 do i = i+1 {if i > 10 { continue } print i} print 1");
  }

  @Test
  public void whileBreak() {
    generateProgram("i=0 while i < 30 do i = i+1 {if i > 10  { break } print i} print -1");
  }

  @Test
  public void whileNestedBreak() {
    generateProgram(
        "i=0 while i < 30 do i = i+1 { "
            + "  j = 0 while j < 10 do j = j + 1 { "
            + "    print j break"
            + "  }"
            + "   if i > 10  { break } print i"
            + "}"
            + "print -1");
  }

  @Test
  public void procVoid() {
    generateProgram("f:proc() {print 'hi'} main{ f() }");
  }

  @Test
  public void procInt() {
    generateProgram("f:proc():int {return 3} main{ x=f() }");
  }

  @Test
  public void procArg() {
    generateProgram("f:proc(n:int, m:int):int {return n+m} main{ a=3 x=f(1, a) f(2,3) }");
  }

  @Test
  public void stringIndex() {
    generateProgram("a='hi' b=a[1]");
  }

  @Test
  public void constStringIndex() {
    generateProgram("a='hi'[1]");
  }

  @Test
  public void stringLength() {
    generateProgram("a=length('hi')");
  }

  @Test
  public void recordFieldSet() {
    generateProgram(
        "rec: record {f:string i:int}\n" //
            + "r = new rec\n" //
            + "r.f = 'hi'");
  }

  private List<Op> generateProgram(String program) {
    Lexer lexer = new Lexer(program);
    State state = State.create(program).build();
    Parser parser = new Parser(lexer);
    state = parser.execute(state);
    if (state.error()) {
      fail(state.errorMessage());
    }
    ProgramNode root = state.programNode();

    StaticChecker checker = new StaticChecker(root);
    TypeCheckResult result = checker.execute();
    if (result.isError()) {
      fail(result.exception().getMessage());
    }
    SymTab table = result.symbolTable();
    CodeGenerator<Op> codegen = new ILCodeGenerator(root, table);
    return codegen.generate();
  }
}
