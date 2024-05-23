package com.plasstech.lang.d2.codegen.x64.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.plasstech.lang.d2.InterpreterExecutor;
import com.plasstech.lang.d2.YetAnotherCompiler;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.x64.NasmCodeGenerator;
import com.plasstech.lang.d2.codegen.x64.optimize.NasmOptimizer;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;

/**
 * I kind of hate how ... random... the methods in this file are.
 */
final public class NasmCodeGeneratorTestUtils {
  private static File dir = Files.createTempDir();

  public static String execute(String sourceCode, String filename) throws Exception {
    String notOptimizedStdOut = assertCompiledEqualsInterpreted(sourceCode, filename, 0, false);
    String optimizedStdOut = assertCompiledEqualsInterpreted(sourceCode, filename, 0, true);
    //    System.out.println("not optimized ");
    //    System.out.println(notOptimizedStdOut);
    //    System.out.println("optimized ");
    //    System.out.println("OUTPUT:");
    //    System.out.println("-------");
    //    System.out.println(optimizedStdOut);
    //
    // Not-optimized is the gold standard.
    assertThat(optimizedStdOut).isEqualTo(notOptimizedStdOut);
    return optimizedStdOut;
  }

  /**
   * Compiles down to x64 executable using the existing state of the configBuilder field inluding
   * the "optimize" flag, runs it through the interpreter and asserts that the compiled output
   * equals the interpreted output. Asserts that the exit code is as given.
   */
  public static String assertCompiledEqualsInterpreted(String sourceCode, String filename,
      int exitCode,
      boolean optimize) throws Exception {
    State compiledState = null;
    try {
      filename = filename + "_opt_" + String.valueOf(optimize);

      CompilationConfiguration config =
          CompilationConfiguration.builder().setFilename(filename).setSourceCode(sourceCode)
              .setCodeGenDebugLevel(2)
              .setOptDebugLevel(2)
              .setOptimize(optimize)
              .build();

      compiledState = compile(config, exitCode);

      InterpreterExecutor ee = new InterpreterExecutor(config);
      // This runs the interpreter on the IL code.
      InterpreterResult result = ee.execute(compiledState);
      compiledState.throwOnError();

      // The compiler converts \n to \r\n, so we have to do the same.
      String interpreterOutput =
          Joiner.on("").join(result.environment().output()).replaceAll("\n", "\r\n");

      System.out.println("compiled optimized " + optimize);
      System.out.println(compiledState.stdOut());
      System.out.println("interpreted: ");
      System.out.println(interpreterOutput);

      // Compiled is the gold standard.
      assertThat(interpreterOutput).isEqualTo(compiledState.stdOut());
      return interpreterOutput;
    } catch (D2RuntimeException e) {
      if (compiledState != null) {
        System.err.println(compiledState.asmCode());
      }
      throw e;
    }
  }

  /**
   * Compiles down to x64 executable using the existing state of the configBuilder field, runs it
   * and asserts that the exit code is 0.
   */
  public static State compile(String sourceCode, String filename) throws Exception {
    CompilationConfiguration config =
        CompilationConfiguration.builder().setSourceCode(sourceCode).setFilename(filename).build();
    return compile(config, 0);
  }

  /**
   * Compiles down to x64 executable, runs it and asserts that the exit code matches.
   */
  private static State compile(CompilationConfiguration config, int exitCode) throws Exception {
    // This parses, type checks, generates IL code, and optionally optimizes.
    YetAnotherCompiler yac = new YetAnotherCompiler();
    State state = yac.compile(config);
    state.throwOnError();

    state = new NasmCodeGenerator().execute(state);
    if (config.expectedErrorPhase() != PhaseName.ASM_CODEGEN) {
      state.throwOnError();
    } else if (state.error()) {
      return state;
    }

    if (config.optimize()) {
      state = new NasmOptimizer().execute(state);
    }

    if (config.codeGenDebugLevel() > 1) {
      String asmCode = Joiner.on('\n').join(state.asmCode());
      System.err.println(asmCode);
    }

    String sourceFilename = config.filename();
    File file = new File(dir, sourceFilename + ".asm");
    if (file.exists()) {
      file.delete();
    }
    file.createNewFile();

    CharSink charSink = Files.asCharSink(file, Charset.defaultCharset(), FileWriteMode.APPEND);
    charSink.writeLines(state.asmCode());

    ProcessBuilder pb = new ProcessBuilder("nasm", "-fwin64", file.getAbsolutePath());
    pb.directory(dir);
    Process process = pb.start();
    process.waitFor();
    assertNoProcessError(process, "nasm", 0);

    File obj = new File(dir, sourceFilename + ".obj");
    File exe = new File(dir, sourceFilename);
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
    String compiledOutput = new String(ByteStreams.toByteArray(stream));
    state = state.addStdOut(compiledOutput);

    assertNoProcessError(process, "Executable", exitCode);
    System.err.println("COMPILED OUTPUT: " + compiledOutput);

    return state;
  }

  private static void assertNoProcessError(Process process, String name, int exitCode)
      throws IOException {
    if (process.exitValue() != exitCode) {
      InputStream stream = process.getErrorStream();
      String output = new String(ByteStreams.toByteArray(stream));
      System.err.printf("%s error output: %s\n", name, output);
      stream = process.getInputStream();
      output = new String(ByteStreams.toByteArray(stream));
      System.err.printf("%s std output: %s\n", name, output);
      assertWithMessage(name + " had wrong exit value: " + output)
          .that(process.exitValue())
          .isEqualTo(0);
    }
  }

  public static void assertGenerateError(String sourceCode, String error) {
    assertGenerateError(sourceCode, error, true, PhaseName.IL_OPTIMIZE);
  }

  public static void assertGenerateError(String sourceCode, String error, boolean optimize,
      PhaseName expectedPhase) {
    CompilationConfiguration config =
        CompilationConfiguration.builder()
            .setSourceCode(sourceCode)
            .setOptimize(optimize)
            .setCodeGenDebugLevel(2)
            .setExpectedErrorPhase(expectedPhase)
            .build();
    YetAnotherCompiler yac = new YetAnotherCompiler();
    State state = yac.compile(config);
    if (state.error()) {
      assertThat(state.errorMessage()).matches(error);
      return;
    }

    if (config.codeGenDebugLevel() > 1) {
      System.err.println(
          String.join(
              "\n", state.lastIlCode().stream().map(Op::toString).collect(toImmutableList())));
    }
    state = new NasmCodeGenerator().execute(state);
    assertThat(state.error()).isTrue();
    assertThat(state.errorMessage()).matches(error);
  }

  public static void assertRuntimeError(String sourceCode, String filename, String error)
      throws Exception {

    CompilationConfiguration config =
        CompilationConfiguration.builder().setSourceCode(sourceCode).setCodeGenDebugLevel(2)
            .setFilename(filename).build();

    assertRuntimeError(config, error);
    assertRuntimeError(
        config.toBuilder().setOptimize(true).setOptDebugLevel(2).build(),
        error);
  }

  public static void assertRuntimeErrorNoOptimize(String sourceCode, String filename,
      String error)
      throws Exception {

    CompilationConfiguration config =
        CompilationConfiguration.builder().setSourceCode(sourceCode).setFilename(filename).build();

    assertRuntimeError(config, error);
  }

  private static void assertRuntimeError(CompilationConfiguration config, String expectedError)
      throws Exception {
    State state = compile(config, -1);

    String compiledOutput = state.stdOut();
    System.out.println("COMPILED OUTPUT (hopefully with error):");
    System.out.println("------------------------------");
    System.out.println(compiledOutput);
    assertThat(compiledOutput).contains(expectedError);
  }
}
