package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.Ignore;
import org.junit.Test;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.testing.TestUtils;

public class NasmCodeGeneratorTest {

  private Process process;

  @Ignore
  @Test
  public void printString() throws Exception {
    execute("print 'printString'", "printString", "printString");
  }

  @Test
  public void printConst() throws Exception {
    execute("print 3", "printConst", "3");
  }

  @Test
  public void printVariable() throws Exception {
    execute("a=3 print a", "printVariable", "3");
  }

  @Test
  public void notVariable() throws Exception {
    execute("a=3 a=!a print a", "notVariable", "3");
  }

  @Test
  @Ignore
  public void addGlobals() throws Exception {
    //    execute("a=1 b=2 c=3 d=4 e=5 f=6 g=a+a*b+(b+c)*d-(c+d)/e+(d-e)*f print g", "addGlobals",
    // "4");
    execute(
        "a=1 b=2 c=3 d=4 e=5 f=6 g=a+(a+b+(b+c+(c+d+(d+e+(e+f+(f))+d)+c)+b)+a) print g",
        "addGlobals",
        "52");
  }

  @Test
  @Ignore
  public void ifPrint() throws Exception {
    execute("a=3 if a > 2 {print a}", "ifPrint", "3");
  }

  @Test
  public void assign() throws Exception {
    execute("assign=3 bassign=assign print bassign print assign", "assign", "33");
  }

  @Test
  public void exit() throws Exception {
    execute("exit", "exit", "");
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
    execute("a=42 a=a+1 print a a=41 a=a-1 print a", "incDec", "4340");
  }

  private void execute(String sourceCode, String filename, String expectedOutput) throws Exception {
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
    process = pb.start();
    process.waitFor();
    if (process.exitValue() != 0) {
      InputStream stream = process.getErrorStream();
      String output = new String(ByteStreams.toByteArray(stream));
      System.err.println("Error: " + output);
      assertThat(process.exitValue()).isEqualTo(0);
    }

    File obj = new File(dir, filename + ".obj");
    File exe = new File(dir, filename);
    pb = new ProcessBuilder("gcc", obj.getAbsolutePath(), "-o", exe.getAbsolutePath());
    pb.directory(dir);
    process = pb.start();
    process.waitFor();
    assertThat(process.exitValue()).isEqualTo(0);

    pb = new ProcessBuilder(exe.getAbsolutePath());
    pb.directory(dir);
    process = pb.start();
    process.waitFor();
    InputStream stream = process.getInputStream();
    String output = new String(ByteStreams.toByteArray(stream));
    assertThat(output).isEqualTo(expectedOutput);
  }
}
