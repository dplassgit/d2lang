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

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.plasstech.lang.d2.Executor;
import com.plasstech.lang.d2.interpreter.ExecutionResult;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.testing.TestUtils;

public class NasmCodeGeneratorTest {
  private static File dir;

  @SuppressWarnings("deprecation")
  @BeforeClass
  public static void setUpClass() throws Exception {
    dir = Files.createTempDir();
  }

  @Ignore
  @Test
  public void printString() throws Exception {
    execute("print 'printString'", "printString");
  }

  @Test
  public void printInt() throws Exception {
    execute("print 3", "printInt" /*heh*/);
  }

  @Test
  public void printBool() throws Exception {
    execute("print true", "printTrue");
    execute("print false", "printFalse");
  }

  @Test
  public void printVariable() throws Exception {
    execute("a=3 print a", "printVariable");
  }

  @Test
  public void notVariable() throws Exception {
    execute("a=3 b=!a print b", "notVariable");
  }

  @Test
  public void negVariable() throws Exception {
    execute("a=3 b=-a print b", "negVariable");
  }

  @Test
  public void binOps() throws Exception {
    for (char c : "+-*/&|^".toCharArray()) {
      execute(
          String.format("a=123 b=-234 c=a%cb print c d=b%ca print d", c, c),
          String.format("binOp%d", (int) c));
    }
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
        "realfib");
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
  @Ignore
  public void divLoop() throws Exception {
    // this fails because we can't compare to numbers > 256 yet
    execute("a=10000 while a > 0 {print a a = a / 10 }", "divLoop");
  }

  @Test
  public void divLoopByte() throws Exception {
    execute("a=100 while a > 0 {print a a = a / 3 }", "divLoopByte");
  }

  @Test
  public void assign() throws Exception {
    execute("a=3 b=a a=4 print b print a", "assign");
  }

  @Test
  public void boolAssign() throws Exception {
    execute("a=true b=a print a print b", "boolAssignTrue");
    execute("a=false b=a print a print b", "boolAssignFalse");
  }

  @Test
  public void exit() throws Exception {
    execute("exit", "exit");
  }

  @Test
  @Ignore
  public void exitErrorGlobal() {
    String sourceCode = "exit 'exitErrorGlobal'";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("exitErrorGlobal");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  @Test
  @Ignore
  public void exitError() {
    String sourceCode = "exit 'exit'";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("exitError");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  @Test
  @Ignore
  public void exitMain() {
    String sourceCode = "main {exit 'exitMain'}";
    State state = TestUtils.compile(sourceCode);
    state = state.addFilename("exitMain");
    state = new NasmCodeGenerator().execute(state);
    System.err.println(Joiner.on('\n').join(state.asmCode()));
    assertThat(state.asmCode()).isNotEmpty();
  }

  @Test
  public void incDec() throws Exception {
    execute("a=42 a=a+1 print a a=41 a=a-1 print a", "incDec");
  }

  private void execute(String sourceCode, String filename) throws Exception {
    State state = TestUtils.compile(sourceCode);
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
    assertNoProcessError(process, "nasm");

    File obj = new File(dir, filename + ".obj");
    File exe = new File(dir, filename);
    pb = new ProcessBuilder("gcc", obj.getAbsolutePath(), "-o", exe.getAbsolutePath());
    pb.directory(dir);
    process = pb.start();
    process.waitFor();
    assertNoProcessError(process, "Linking");

    pb = new ProcessBuilder(exe.getAbsolutePath());
    pb.directory(dir);
    process = pb.start();
    process.waitFor();
    InputStream stream = process.getInputStream();
    assertNoProcessError(process, "Executable");

    String compiledOutput = new String(ByteStreams.toByteArray(stream));

    Executor ee = new Executor(sourceCode);
    ee.setOptimize(true);
    ExecutionResult result = ee.execute();
    String interpreterOutput = Joiner.on("").join(result.environment().output());

    assertThat(compiledOutput).isEqualTo(interpreterOutput);
  }

  private void assertNoProcessError(Process process, String name) throws IOException {
    if (process.exitValue() != 0) {
      InputStream stream = process.getErrorStream();
      String output = new String(ByteStreams.toByteArray(stream));
      System.err.printf("%s output: %s\n", name, output);
      assertWithMessage(name + " had wrong exit value").that(process.exitValue()).isEqualTo(0);
    }
  }
}
