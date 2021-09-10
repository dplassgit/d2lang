package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.Executor;
import com.plasstech.lang.d2.interpreter.ExecutionResult;
import com.plasstech.lang.d2.phase.State;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorTest {
  private static File dir;

  @TestParameter boolean optimize;

  @SuppressWarnings("deprecation")
  @BeforeClass
  public static void setUpClass() throws Exception {
    dir = Files.createTempDir();
  }

  @Test
  public void printDuplicateStrings() throws Exception {
    execute("print 'hello' print 'world' print 'hello world'", "printDuplicateStrings");
  }

  @Test
  public void printLn() throws Exception {
    execute("println 'hello world'", "println");
    execute("println 'println with cr\\r' println 'println with newline\\n'", "printlnWithCrLf");
    execute("print 'print\"ln' println '\"mixed'", "printlnMixed");
  }

  @Test
  public void printInt() throws Exception {
    execute("print 3", "printInt" /*heh*/);
    execute("print -3", "printNegInt");
  }

  @Test
  public void printBool(@TestParameter boolean bool) throws Exception {
    execute("print " + bool, "print" + bool);
  }

  @Test
  public void printIntVariable() throws Exception {
    execute("a=3 print a", "printIntVariable");
  }

  @Test
  public void evilVariableName() throws Exception {
    execute("rax=3 print rax", "evilVariableName");
  }

  @Test
  public void printStringVariable() throws Exception {
    execute("a='hello' print a", "printStringVariable");
  }

  @Test
  public void intUnaryOps(@TestParameter({"-", "!"}) String op) throws Exception {
    execute(String.format("a=3 b=%sa print b", op), "intUnaryOps");
  }

  @Test
  public void intBinOps(
      @TestParameter({"+", "-", "*", "/", "&", "|", "^", "%"}) String op,
      @TestParameter({"1234", "-234567"}) int first,
      @TestParameter({"1234", "-234567"}) int second)
      throws Exception {
    execute(
        String.format(
            "a=%d b=%d c=a %s b print c d=b %s a print d e=a %s a print e f=b %s b print f",
            first, second, op, op, op, op),
        "intBinOps");
  }

  @Test
  public void intCompOps(
      @TestParameter({"<", "<=", "==", "!=", ">=", ">"}) String op,
      @TestParameter({"0", "1234", "-34567"}) int first,
      @TestParameter({"0", "1234", "-34567"}) int second)
      throws Exception {
    execute(
        String.format(
            "      a=%d b=%d " //
                + "c=a %s b print c " //
                + "d=a %s a print d",
            first, second, op, op),
        "intCompOps");
  }

  @Test
  public void shiftOps(@TestParameter({"<<", ">>"}) String op) throws Exception {
    execute(String.format("a=123 b=4 c=a%sb print c a=-234 d=b%sa print d", op, op), "shiftOps");
  }

  @Test
  public void tree() throws Exception {
    execute(
        "      a=2 "
            + "b=3 "
            + "c=-5 "
            + "d=7 "
            + "e=11 "
            + "f=13 "
            + "g = (((a+b+c+2)*(b+c+d+1))*((c-d-e-1)/(d-e-f-2))*((e+f+a-3)*"
            + "      (f-a-b+4)))*((a+c+e-9)/(b-d-f+11)) "
            + "print g",
        "tree");
  }

  @Test
  public void allOpsGlobals() throws Exception {
    execute(
        "      a=2 "
            + "b=3 "
            + "c=-5 "
            + "d=7 "
            + "e=11 "
            + "f=13 "
            + "z=0"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)+a)*b)/c)-d)) print g"
            + " k=z+4/(5+(4-5*f)) print k"
            + " k=0+d/(5+(4-5*f)) print k"
            + " g=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f)))))) print g"
            + " h=0+a+(4+3*(4-(3+4/(4+(5-e*6))))) print h"
            + " j=a+a*(b+(b+c*(d-(c+d/(e+(d-e*f+0)))))) print j"
            + " aa=2+a*(3+(3+5*(7-(5+7/11)+(7-11*13))*2)/b) print aa"
            + "",
        "allOpsGlobal");
  }

  @Test
  public void rounding() throws Exception {
    execute("f=6 k=4/(5+(4-5*f)) print k", "rounding");
  }

  @Test
  @Ignore
  public void allOpsLocals() throws Exception {
    execute(
        "      a=1 b=2 c=3 d=4 e=5 f=6 g=3\n"
            + "fun:proc():int { \n"
            + "   g=a+a*b+(b+c)*d-(c+d)/e+(d-e)*f  \n"
            + "   return g \n"
            + "} \n"
            + "print fun()",
        "allOpsLocals");
  }

  @Test
  public void ifPrint() throws Exception {
    execute("a=3 if a > 1 {print a}", "ifPrint");
  }

  @Test
  public void fib() throws Exception {
    // DANG THIS WORKS!
    execute(
        "      n1 = 0 "
            + "n2 = 1 "
            + "i=1 while i <= 10 do i = i + 1 {"
            + "  nth = n1 + n2"
            + "  n1 = n2"
            + "  n2 = nth"
            + "}"
            + "print nth",
        "fib");
  }

  @Test
  public void fib0() throws Exception {
    execute(
        "n=10\r\n"
            + "n1 = 0\r\n"
            + "n2 = 1\r\n"
            + "i=1 while i <= n do i = i+1 {\r\n"
            + "  nth = n1 + n2\r\n"
            + "  n1 = n2\r\n"
            + "  n2 = nth\r\n"
            + "  print i\r\n"
            + "  print \"th fib: \"\r\n"
            + "  println nth\r\n"
            + "}\r\n"
            + "println '' // NOTE: cannot just do PRINT (no expression)...\r\n"
            + "print \"Final fib: \"\r\n"
            + "println nth",
        "fib0");
  }

  @Test
  public void fact() throws Exception {
    execute(
        "      fact = 1 "
            + "i=1 while i <= 10 do i = i + 1 {"
            + "  fact = fact * i"
            + "}"
            + "print fact",
        "fact");
  }

  @Test
  public void addToItself() throws Exception {
    execute("a=3 a=a+10 print a", "addToItself");
  }

  @Test
  public void divLoop() throws Exception {
    execute("a=10000 while a > 0 {print a a = a / 10 }", "divLoop");
  }

  @Test
  public void divLoopByte() throws Exception {
    execute("a=100 while a > 0 {print a a = a / 3 }", "divLoopByte");
  }

  @Test
  public void assignInt() throws Exception {
    execute("a=3 b=a a=4 print b print a", "assignInt");
  }

  @Test
  public void assignString(
      @TestParameter({"s", "hello", "hello this is a very long string"}) String value)
      throws Exception {
    execute(String.format("a='%s' b=a print b", value), "assignString");
  }

  @Test
  public void boolAssign(@TestParameter boolean bool) throws Exception {
    execute(String.format("a=%s b=a print a print b", bool), "boolAssign" + bool);
  }

  @Test
  public void not(@TestParameter boolean bool) throws Exception {
    execute(String.format("a=%s c=not a print a print c", bool), "not" + bool);
  }

  @Test
  public void boolBinOp(
      @TestParameter({"and", "or", "xor", "<", "<=", "==", "!=", ">", ">="}) String op,
      @TestParameter boolean boola,
      @TestParameter boolean boolb)
      throws Exception {
    execute(
        String.format("a=%s b=%s c=a %s b print c d=b %s a print d", boola, boolb, op, op),
        "boolBinOp" + boola + boolb);
  }

  @Test
  public void exit() throws Exception {
    execute("exit", "exit");
  }

  @Test
  public void exitErrorConst() throws Exception {
    execute("exit 'exitErrorConst'", "exitErrorConst", -1);
  }

  @Test
  public void exitErrorVariable() throws Exception {
    execute("a='exitErrorVariable' exit a", "exitErrorVariable", -1);
  }

  @Test
  @Ignore
  public void exitMain() throws Exception {
    execute("main {exit 'exitMain'}", "exitMain");
  }

  @Test
  public void incDec() throws Exception {
    execute("a=42 a=a+1 print a a=41 a=a-1 print a", "incDec");
  }

  @Test
  public void constStringLength(
      @TestParameter({"s", "hello", "hello this is a very long string"}) String value)
      throws Exception {
    execute(String.format("b=length('hello' + '%s') print b", value), "constStringLength");
  }

  @Test
  public void stringLength(
      @TestParameter({"", "s", "hello", "hello this is a very long string"}) String value)
      throws Exception {
    execute(String.format("a='%s' c='lo' b=length(c)+length(a) print b", value), "stringLength");
  }

  @Test
  public void asc(@TestParameter({"s", "hello", "hello this is a very long string"}) String value)
      throws Exception {
    execute(String.format("a='%s' b=asc(a) print b", value), "asc");
    execute(String.format("b=asc('%s') print b", value), "ascConst");
    execute(String.format("a='%s' b=a c=asc(b) print c", value), "asc2");
  }

  @Test
  public void stringIndex(
      @TestParameter({"world", "hello this is a very long string"}) String value,
      @TestParameter({"1", "4"}) int index)
      throws Exception {
    execute(
        String.format("i=%d a='%s' b=a[i] print b c=a[%d] print c", index, value, index),
        "stringIndex");
  }

  @Test
  public void constantStringIndex(
      @TestParameter({"world", "hello this is a very long string"}) String value,
      @TestParameter({"1", "4"}) int index)
      throws Exception {
    execute(
        String.format("i=%d b='%s'[i] print b c='%s'[%d] print c", index, value, value, index),
        "constantStringIndex");
  }

  @Test
  public void printParse() throws Exception {
    execute(
        "print 123 print ', '\n" //
            + " print 'should be b:'\n" //
            + " Println 'abcde'[1]",
        "printParse");
  }

  @Test
  public void chr(@TestParameter({"65", "96"}) int value) throws Exception {
    execute(String.format("a=%d b=chr(a) print b", value), "chr");
    execute(String.format("a=chr(%d) print a", value), "chrConst");
  }

  @Test
  public void stringAddSimple() throws Exception {
    execute("a='a' c=a+'b' print c", "stringAddSimple");
  }

  @Test
  public void stringAddComplex() throws Exception {
    execute(
        "a='abc' b='def' c=a+b print c d=c+'xyz' print d e='ijk'+d+chr(32) print e",
        "stringAddComplex");
  }

  private void execute(String sourceCode, String filename) throws Exception {
    execute(sourceCode, filename, 0);
  }

  private void execute(String sourceCode, String filename, int exitCode) throws Exception {
    filename = filename + "_opt_" + String.valueOf(optimize);

    Executor ee = new Executor(sourceCode);
    ee.setOptimize(optimize);
    ExecutionResult result = ee.execute();
    State state = ee.state();
    System.err.println(Joiner.on('\n').join(state.lastIlCode()));
    state = state.addFilename(filename);

    state = new NasmCodeGenerator().execute(state);
    String asmCode = Joiner.on('\n').join(state.asmCode());
    System.err.println(asmCode);

    File file = new File(dir, filename + ".asm");
    if (file.exists()) {
      file.delete();
    }
    file.createNewFile();

    System.err.printf("FILE IS AT %s\n", file.getAbsolutePath());
    CharSink charSink = Files.asCharSink(file, Charset.defaultCharset(), FileWriteMode.APPEND);
    charSink.writeLines(state.asmCode());

    ProcessBuilder pb = new ProcessBuilder("nasm", "-fwin64", file.getAbsolutePath());
    pb.directory(dir);
    Process process = pb.start();
    process.waitFor();
    assertNoProcessError(process, "nasm", 0);

    File obj = new File(dir, filename + ".obj");
    File exe = new File(dir, filename);
    pb = new ProcessBuilder("gcc", obj.getAbsolutePath(), "-o", exe.getAbsolutePath());
    pb.directory(dir);
    process = pb.start();
    process.waitFor();
    assertNoProcessError(process, "Linking", 0);

    pb = new ProcessBuilder(exe.getAbsolutePath());
    pb.directory(dir);
    process = pb.start();
    process.waitFor();
    InputStream stream = process.getInputStream();
    assertNoProcessError(process, "Executable", exitCode);

    String compiledOutput = new String(ByteStreams.toByteArray(stream));

    // the compiler converts \n to \r\n, so we have to do the same.
    String interpreterOutput =
        Joiner.on("").join(result.environment().output()).replaceAll("\n", "\r\n");

    assertThat(compiledOutput).isEqualTo(interpreterOutput);
  }

  private void assertNoProcessError(Process process, String name, int exitCode) throws IOException {
    if (process.exitValue() != exitCode) {
      InputStream stream = process.getErrorStream();
      String output = new String(ByteStreams.toByteArray(stream));
      System.err.printf("%s output: %s\n", name, output);
      assertWithMessage(name + " had wrong exit value").that(process.exitValue()).isEqualTo(0);
    }
  }
}
