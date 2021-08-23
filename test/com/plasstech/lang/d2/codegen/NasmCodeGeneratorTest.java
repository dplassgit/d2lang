package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

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

  @Ignore
  @Test
  public void printString() throws Exception {
    execute("print 'printString'", "printString");
  }

  @Test
  public void printInt /*heh*/() throws Exception {
    execute("print 3", "printInt");
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
  public void addGlobals() throws Exception {
    //    execute("a=1 b=2 c=3 d=4 e=5 f=6 g=a+a*b+(b+c)*d-(c+d)/e+(d-e)*f print g", "addGlobals",
    // "4");
    //    execute(
    //        "a=1 b=2 c=3 d=4 e=5 f=6 g=a+(a+b+(b+c+(c+d+(d+e+(e+f+(f))+d)+c)+b)+a) print g",
    //        "addGlobals");
    execute("a=1 b=2 c=3 d=4 e=5 f=6 g=a+(b+(c+(d+(e+(f+(f))+d)+c)+b)+a) print g", "addGlobals");
  }

  @Test
  public void ifPrint() throws Exception {
    execute("a=3 if a > 1+4 {print a}", "ifPrint");
  }

  @Test
  public void fib() throws Exception {
    execute(
        "a=3 fib=0 while a > 0 do a = a - 1 { print a print fib fib = fib + 2} print fib", "fib");
  }

  @Test
  public void add() throws Exception {
    execute("a=3 b=a+3 c=a+4 d=c+b print d", "add");
  }

  @Test
  public void mul() throws Exception {
    execute("a=3 b=a+3 c=9*b print c", "mul");
  }

  @Test
  public void assign() throws Exception {
    execute("a=3 b=a a=4 print b print a", "assign");
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

    @SuppressWarnings("deprecation")
    File dir = Files.createTempDir();
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
