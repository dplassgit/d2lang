package com.plasstech.lang.d2.interpreter;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.ExecutionEnvironment;

public class InterpreterTest {
  @Test
  public void stringTest() {
    Environment env = execute("i='hi' println i");
    assertThat(env.getValue("i")).isEqualTo("hi");
    assertThat(env.output()).containsExactly("hi", "\n");
  }

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
    Environment env =
        execute(
            "      n=10\n"
                + "n1 = 0\n"
                + "n2 = 1\n"
                + "i=0 while i < n*2-1 do i = i+1 {\n"
                + "  if (i%2)==0 {\n"
                + "    continue\n"
                + "  }\n"
                + "  nth = n1 + n2\n"
                + "  n1 = n2\n"
                + "  n2 = nth\n"
                + "  print nth\n"
                + "}\n"
                + "");
    assertThat(env.getValue("nth")).isEqualTo(55);
    assertThat(env.getValue("i")).isEqualTo(19);
  }

  @Test
  public void fact() {
    Environment env =
        execute(
            "      n=10 "
                + "fact = 1 "
                + "i=1 while i <= n do i = i+1 { "
                + "  fact = fact*i "
                + "  print fact "
                + "}");

    assertThat(env.getValue("i")).isEqualTo(11);
    assertThat(env.getValue("fact")).isEqualTo(3628800);
  }

  @Test
  public void ifElse() {
    Environment env =
        execute(
            "      n = 0 "
                + "while n < 10 do n = n + 1 {"
                + " if n == 1 { print -1 } "
                + " elif (n == 2) { print -2 } "
                + " else {"
                + "   if n==3 {print -3} "
                + "   else {print n}}"
                + "}");
    assertThat(env.getValue("n")).isEqualTo(10);
  }

  @Test
  public void bools() {
    Environment env = execute("a=true b = !a");

    assertThat(env.getValue("a")).isEqualTo(1);
    assertThat(env.getValue("b")).isEqualTo(0);
  }

  @Test
  public void procCall() {
    Environment env = execute("f:proc(a:int):int { print a return a+1} main{x=f(3)}");
    assertThat(env.getValue("x")).isEqualTo(4);
  }

  @Test
  public void recursiveProcCall() {
    Environment env =
        execute(
            "      f:proc(a:int):int { "
                + "  if a==1 { return a } "
                + "  else {return a*f(a-1)}} "
                + "main{x=f(3)}");
    assertThat(env.getValue("x")).isEqualTo(6);
  }

  @Test
  public void dualRecursiveProcCall() {
    Environment env =
        execute(
            "      fib: proc(n: int) : int {\n"
                + "  if n <= 1 {\n"
                + "    return n \n"
                + "  } else {\n"
                + "    return fib(n - 1) + fib(n - 2)\n"
                + "  }\n"
                + "}\n"
                + "x = fib(10)");
    assertThat(env.getValue("x")).isEqualTo(55);
  }

  @Test
  public void stringIndex() {
    Environment env = execute("a='hi' b=a[1]");
    assertThat(env.getValue("a")).isEqualTo("hi");
    assertThat(env.getValue("b")).isEqualTo("i");
  }

  @Test
  public void constStringIndex() {
    Environment env = execute("a='hi'[1]");
    assertThat(env.getValue("a")).isEqualTo("i");
  }

  @Test
  public void stringAdd() {
    Environment env = execute("a='hi' b=a + ' there'");
    assertThat(env.getValue("a")).isEqualTo("hi");
    assertThat(env.getValue("b")).isEqualTo("hi there");
  }

  @Test
  public void stringMultipleAdds() {
    Environment env = execute("a='hi' b=a + ' ' + a");
    assertThat(env.getValue("a")).isEqualTo("hi");
    assertThat(env.getValue("b")).isEqualTo("hi hi");
  }

  @Test
  public void stringAddSelf() {
    Environment env = execute("a='hi ' a = a + a");
    assertThat(env.getValue("a")).isEqualTo("hi hi ");
  }

  @Test
  public void arrayIter() {
    Environment env = execute("a=[2,4,6] i=0 while i < 3 do i = i + 1 { print a[i] }");
    assertThat(env.getValue("a")).isEqualTo(new Integer[] {2, 4, 6});
    assertThat(env.output()).containsExactly("2", "4", "6");
  }

  @Test
  public void compareString() {
    Environment env =
        execute(
            "      isDigit: proc(c: string): bool { return c >= '0' & c <= '9' }"
                + "a = isDigit(' ')"
                + "b = isDigit('0')"
                + "c = isDigit('1')"
                + "d = isDigit('9')"
                + "e = isDigit('z')"
                + "");
    assertThat(env.getValue("a")).isEqualTo(0);
    assertThat(env.getValue("b")).isEqualTo(1);
    assertThat(env.getValue("c")).isEqualTo(1);
    assertThat(env.getValue("d")).isEqualTo(1);
    assertThat(env.getValue("e")).isEqualTo(0);
  }

  @Test
  public void globalReference() {
    // Tests bug#39
    Environment env =
        execute(
            "      a:string a='bye'"
                + "setup: proc {"
                + "  a = 'hi'"
                + "  b = 'bee'"
                + "}"
                + "main {"
                + "  setup()"
                + "}");
    assertThat(env.getValue("a")).isEqualTo("hi");
    assertThat(env.getValue("b")).isNull();
  }

  @Test
  public void lengths() {
    Environment env = execute("a=length('hi') b=length([1,2,3])");
    assertThat(env.getValue("a")).isEqualTo(2);
    assertThat(env.getValue("b")).isEqualTo(3);
  }

  @Test
  public void ascAndChr() {
    Environment env = execute("a=asc('A') b=chr(66) c=asc(chr(67)) d=chr(asc('D'))");
    assertThat(env.getValue("a")).isEqualTo(65);
    assertThat(env.getValue("b")).isEqualTo("B");
    assertThat(env.getValue("c")).isEqualTo(67);
    assertThat(env.getValue("d")).isEqualTo("D");
  }

  @Test
  public void ignoreReturnValue() {
    execute("a:proc(): string { return 'aproc' } a()");
  }

  @Test
  public void returnNotNull() {
    Environment env = execute("a:proc(): int { return asc('b') } b=a()");
    assertThat(env.getValue("b")).isEqualTo(98);

    env = execute("a:proc(): int { return length('abc') } b=a()");
    assertThat(env.getValue("b")).isEqualTo(3);

    env = execute("a:proc(): string { return chr(66) } b=a()");
    assertThat(env.getValue("b")).isEqualTo("B");

    env = execute("a:proc(): bool { return true } b=a()");
    assertThat(env.getValue("b")).isEqualTo(1);

    env = execute("a:proc(): bool { return false } b=a()");
    assertThat(env.getValue("b")).isEqualTo(0);
  }

  private Environment execute(String program) {
    ExecutionEnvironment ee = new ExecutionEnvironment(program, false);
    Environment env = ee.execute();
    System.out.println(ee.programNode());

    System.out.println("Environment:");
    System.out.println("------------");
    System.out.println(env.toString());
    System.out.println("------------");
    System.out.println("Sysout:");
    System.out.println("-------");
    System.out.println(Joiner.on('\n').join(env.output()));
    return env;
  }
}
