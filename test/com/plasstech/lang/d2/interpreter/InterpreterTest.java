package com.plasstech.lang.d2.interpreter;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.InterpreterExecutor;

@RunWith(TestParameterInjector.class)
public class InterpreterTest {
  @Test
  public void stringTest() {
    Environment env = execute("i='hi' println i");
    assertThat(env.getValue("i")).isEqualTo("hi");
    assertThat(env.output()).containsExactly("hi", "\n");
  }

  @Test
  public void stringCompare() {
    Environment env = execute("i='hi'=='hi' j = 'hi'!='hi' k = 'hi'!='bye' a='hi'=='bye'");
    assertThat(env.getValue("i")).isEqualTo(true);
    assertThat(env.getValue("j")).isEqualTo(false);
    assertThat(env.getValue("k")).isEqualTo(true);
    assertThat(env.getValue("a")).isEqualTo(false);
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
                + "}");
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
    Environment env = execute("a=true b = not a");

    assertThat(env.getValue("a")).isEqualTo(true);
    assertThat(env.getValue("b")).isEqualTo(false);
  }

  @Test
  public void procCall() {
    Environment env = execute("f:proc(a:int):int { print a return a+1} x=f(3)");
    assertThat(env.getValue("x")).isEqualTo(4);
  }

  @Test
  public void proc5Params(@TestParameter boolean optimize) {
    Environment env =
        execute(
            "       add5:proc(a:int,b:int,c:int,d:int,e:int):int {return a+b+c+d+e}"
                + " x=add5(1,2,3,4,5) ",
            optimize);
    assertThat(env.getValue("x")).isEqualTo(1 + 2 + 3 + 4 + 5);
  }

  @Test
  public void recursiveProcCall() {
    Environment env =
        execute(
            "      f:proc(a:int):int { "
                + "  if a==1 { return a } "
                + "  else {return a*f(a-1)}} "
                + "x=f(3)");
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
  public void arraySet() {
    Environment env = execute("a:int[2] a[1]=1 print a[1]");
    assertThat(env.getValue("a")).isEqualTo(new Integer[] {0, 1});
    assertThat(env.output()).containsExactly("1");
  }

  @Test
  public void stringArraySet() {
    Environment env = execute("a:string[2] a[1]='hi' print a[1]");
    assertThat(env.getValue("a")).isEqualTo(new String[] {"", "hi"});
    assertThat(env.output()).containsExactly("hi");
  }

  @Test
  public void boolArraySet() {
    Environment env = execute("a:bool[2] a[1]=true print a[1]");
    assertThat(env.getValue("a")).isEqualTo(new Boolean[] {false, true});
    assertThat(env.output()).containsExactly("true");
  }

  @Test
  public void compareString() {
    Environment env =
        execute(
            "      isDigit: proc(c: string): bool { return c >= '0' and c <= '9' }"
                + "a = isDigit(' ')"
                + "b = isDigit('0')"
                + "c = isDigit('1')"
                + "d = isDigit('9')"
                + "e = isDigit('z')"
                + "");
    assertThat(env.getValue("a")).isEqualTo(false);
    assertThat(env.getValue("b")).isEqualTo(true);
    assertThat(env.getValue("c")).isEqualTo(true);
    assertThat(env.getValue("d")).isEqualTo(true);
    assertThat(env.getValue("e")).isEqualTo(false);
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
                + "setup()");
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
    assertThat(env.getValue("b")).isEqualTo(true);

    env = execute("a:proc(): bool { return false } b=a()");
    assertThat(env.getValue("b")).isEqualTo(false);
  }

  @Test
  public void bitOperations() {
    Environment env = execute("a=123 b=a&64 c=a|31 d=!a");
    assertThat(env.getValue("a")).isEqualTo(123);
    assertThat(env.getValue("b")).isEqualTo(123 & 64);
    assertThat(env.getValue("c")).isEqualTo(123 | 31);
    assertThat(env.getValue("d")).isEqualTo(~123);
  }

  @Test
  public void records() {
    Environment env =
        execute(
            "      r: record { i: int s: string}\n"
                + "an_r = new r \n"
                + "an_r.i = 3 \n"
                + " an_r.s = 'hi' \n"
                + "b = an_r.i "
                + " c = an_r.s \n"
                + " println an_r.i \n"
                + " println an_r.s \n"
                + " println b \n"
                + " println c \n");
    assertThat(env.getValue("b")).isEqualTo(3);
    assertThat(env.getValue("c")).isEqualTo("hi");
  }

  @Test
  public void arrayInRecord() {
    Environment env =
        execute(
            "      r: record { i: int a:double[2]}\n"
                + "an_r = new r \n"
                + "da = an_r.a \n"
                + "da[1] = 2.0 "
                + "e=da[1] \n"
                + "println da ");
    assertThat(env.getValue("e")).isEqualTo(2.0);
  }

  @Test
  public void recordNullField() {
    Environment env =
        execute(
            "      r: record { i: int s: string}\n"
                + "an_r = new r \n"
                + "s = an_r.s \n"
                + "println s \n",
            true);
    assertThat(env.getValue("s")).isNull();
  }

  @Test
  public void recordInProc() {
    Environment env =
        execute(
            "      Parser: record {token:string}"
                + "new_parser: proc(): Parser {"
                + "  parser = new Parser "
                + "  advance_parser(parser) "
                + "  return parser"
                + "}"
                + "advance_parser: proc(it: Parser) {"
                + "  prev = it.token"
                + "  it.token = 'hi'"
                //                + "  return prev"
                + "}"
                + "p=new_parser()"
                + "print p.token",
            true);
    assertThat(env.getValue("p")).isNotNull();
  }

  @Test
  public void nullTest() {
    Environment env = execute("a = null println a==null");
    assertThat(env.getValue("a")).isNull();
    env = execute("a = null println a==null", true);
    assertThat(env.getValue("a")).isNull();
  }

  @Test
  public void allocArray() {
    execute("a:int[2] print a[0]");
  }

  @Test
  public void doubleUnary() throws Exception {
    execute("a=3.0 b=-a print b", false);
    execute("a=3.0 b=-a print b", true);
  }

  @Test
  public void doubleBinOps(
      @TestParameter({"+", "-", "*", "/"}) String op,
      @TestParameter({"1234.5", "-234567.8"}) double first,
      @TestParameter({"-1234.5", "234567.8"}) double second)
      throws Exception {
    execute(
        String.format(
            "a=%f b=%f c=a %s b print c d=b %s a print d e=a %s a print e f=b %s b print f",
            first, second, op, op, op, op),
        false);
  }

  @Test
  public void doubleCompOps(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"0.0", "1234.5", "-34567.8"}) double first,
      @TestParameter({"0.0", "-1234.5", "34567.8"}) double second)
      throws Exception {
    execute(
        String.format(
            "      a=%f b=%f " //
                + "c=a %s b print c " //
                + "d=b %s a print d",
            first, second, op, op),
        false);
  }

  @Test
  public void rounding() throws Exception {
    Environment environment = execute("f=6.0 k=4./(5.+(4.-5.*f)) print k", false);
    assertThat(environment.getValue("f")).isEqualTo(6.0);
    assertThat(environment.getValue("k")).isEqualTo(4. / (5. + (4. - 5. * 6.)));
  }

  @Test
  public void addToItself() throws Exception {
    execute("a=3.1 a=a+10.1 print a", false);
  }

  @Test
  public void assignDouble() throws Exception {
    execute("a=3.14 b=a print b print a", false);
    execute("a=3.24 b=a print b print a", true);
  }

  private Environment execute(String program, boolean optimize) {
    InterpreterExecutor ee = new InterpreterExecutor(program);
    ee.setCodeGenDebugLevel(2);
    ee.setOptDebugLevel(2);
    ee.setOptimize(optimize);
    InterpreterResult result = ee.execute();
    //    System.out.println(ee.programNode());
    //
    //    System.out.println("Environment:");
    //    System.out.println("------------");
    //    Envionment env = result.environment();
    //    System.out.println(env.toString());
    //    System.out.println("------------");
    //    System.out.println("Sysout:");
    //    System.out.println("-------");
    //    System.out.println(Joiner.on('\n').join(env.output()));
    return result.environment();
  }

  private Environment execute(String program) {
    return execute(program, false);
  }
}
