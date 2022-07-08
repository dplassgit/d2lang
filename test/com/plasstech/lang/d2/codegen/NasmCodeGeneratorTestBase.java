package com.plasstech.lang.d2.codegen;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import org.junit.BeforeClass;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.plasstech.lang.d2.InterpreterExecutor;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.optimize.ILOptimizer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;

public class NasmCodeGeneratorTestBase {
  private static File dir;

  @TestParameter boolean optimize;
  //  boolean optimize = false;

  @SuppressWarnings("deprecation")
  @BeforeClass
  public static void setUpClass() throws Exception {
    dir = Files.createTempDir();
  }

  public void execute(String sourceCode, String filename) throws Exception {
    assertCompiledEqualsInterpreted(sourceCode, filename, 0);
  }

  public State compileToNasm(String sourceCode) {
    InterpreterExecutor ee = new InterpreterExecutor(sourceCode);
    ee.setParseDebugLevel(2);
    ee.setCodeGenDebugLevel(1);
    ee.setOptimize(optimize);
    // Compiles and interprets
    ee.execute();
    State state = ee.state();

    state = new NasmCodeGenerator().execute(state);
    System.out.println("\nNASM:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("\n").join(state.asmCode()));
    System.out.println();

    return state;
  }

  public void assertCompiledEqualsInterpreted(String sourceCode, String filename, int exitCode)
      throws Exception {
    filename = filename + "_opt_" + String.valueOf(optimize);

    InterpreterExecutor ee = new InterpreterExecutor(sourceCode);
    ee.setOptimize(optimize);
    InterpreterResult result = ee.execute();
    State state = ee.state();
    //    System.err.println(Joiner.on('\n').join(state.lastIlCode()));
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
      assertWithMessage(name + " had wrong exit value: " + output)
          .that(process.exitValue())
          .isEqualTo(0);
    }
  }

  protected void assertGenerateError(String sourceCode, String error) {
    State state = State.create(sourceCode).build();
    Lexer lexer = new Lexer(state.sourceCode());
    Parser parser = new Parser(lexer);
    state = parser.execute(state);
    if (state.error()) {
      throw state.exception();
    }

    StaticChecker checker = new StaticChecker();
    state = checker.execute(state);
    if (state.error()) {
      throw state.exception();
    }

    ILCodeGenerator codegen = new ILCodeGenerator();
    state = codegen.execute(state);
    if (optimize) {
      // Runs all the optimizers.
      ILOptimizer optimizer = new ILOptimizer();
      state = optimizer.execute(state);
    }

    System.err.println(
        String.join(
            "\n", state.lastIlCode().stream().map(Op::toString).collect(toImmutableList())));
    state = new NasmCodeGenerator().execute(state);
    assertThat(state.error()).isTrue();
    assertThat(state.errorMessage()).contains(error);
  }
}
