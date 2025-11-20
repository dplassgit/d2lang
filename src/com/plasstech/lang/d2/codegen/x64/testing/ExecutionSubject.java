package com.plasstech.lang.d2.codegen.x64.testing;

import static com.google.common.truth.Truth.assertAbout;
import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSink;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import com.plasstech.lang.d2.InterpreterExecutor;
import com.plasstech.lang.d2.YetAnotherCompiler;
import com.plasstech.lang.d2.codegen.x64.NasmCodeGenerator;
import com.plasstech.lang.d2.codegen.x64.optimize.NasmOptimizer;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.phase.State;

public class ExecutionSubject extends Subject {
  private static final File dir = Files.createTempDir();

  public static ExecutionSubject assertThatCompiling(String code) {
    return assertAbout(ExecutionSubject::new).that(code);
  }

  private CompilationConfiguration config;
  private Optional<String> expectedRuntimeError = Optional.empty();

  private ExecutionSubject(FailureMetadata metadata, String code) {
    super(metadata, code);

    this.config =
        CompilationConfiguration.create(code).toBuilder()
            // Default to NOT optimized
            .setOptimize(false)
            .setCodeGenDebugLevel(2)
            .setOptDebugLevel(2)
            .setFilename("sut")
            .build();
  }

  public ExecutionSubject withOptimize(boolean newFlag) {
    this.config = this.config.toBuilder().setOptimize(newFlag).build();
    return this;
  }

  public ExecutionSubject withCodeGenDebugLevel(int level) {
    this.config = this.config.toBuilder().setCodeGenDebugLevel(level).build();
    return this;
  }

  public ExecutionSubject withOptDebugLevel(int level) {
    this.config = this.config.toBuilder().setOptDebugLevel(level).build();
    return this;
  }

  public ExecutionSubject withRuntimeError(String error) {
    this.expectedRuntimeError = Optional.of(error);
    return this;
  }

  public ExecutionSubject withRuntimeChecks(boolean check) {
    this.config = this.config.toBuilder().setRuntimeChecks(check).build();
    return this;
  }

  public void hasCompileTimeError(String error) {
    YetAnotherCompiler compiler = new YetAnotherCompiler();
    State state = compiler.compile(this.config);
    assertThat(state.error()).isTrue();
    assertThat(state.errorMessage()).matches(error);
    System.err.printf("Compile time exception: %s\n", state.exception());
  }

  // Uses the opposite optimization flag as executed
  public State executedEqualsInterpreted(boolean interpretedOptimizeFlag) {
    this.config = this.config.toBuilder().setOptimize(!interpretedOptimizeFlag).build();
    // 1. compile & execute the original source
    State executeState = executes();

    // 2. interpret the original source with the new optimize flag
    CompilationConfiguration interpreterConfig =
        this.config.toBuilder().setOptDebugLevel(0).setOptimize(interpretedOptimizeFlag).build();
    InterpreterExecutor executor = new InterpreterExecutor(interpreterConfig);
    InterpreterResult result = executor.execute();
    String interpreterOutput =
        Joiner.on("").join(result.environment().output()).replaceAll("\n", "\r\n");
    assertThat(executeState.stdOut()).isEqualTo(interpreterOutput);
    return executeState;
  }

  // Uses the opposite optimization flag as executed
  public State executedEqualsInterpreted() {
    return executedEqualsInterpreted(!config.optimize());
  }

  // Compile & execute the original source
  public State executes() {
    YetAnotherCompiler compiler = new YetAnotherCompiler();
    State state = compiler.compile(config);
    state.throwOnError();

    state = new NasmCodeGenerator().execute(state);
    state.throwOnError();

    if (config.optimize()) {
      if (config.codeGenDebugLevel() > 0) {
        String asmCode = Joiner.on('\n').join(state.asmCode());
        System.err.println("\nPRE-ASM OPTIMIZED:\n");
        System.err.println(asmCode);
      }
      state = new NasmOptimizer().execute(state);
    }

    if (config.codeGenDebugLevel() > 0) {
      System.err.println("\nFINAL-ASM:\n");
      String asmCode = Joiner.on('\n').join(state.asmCode());
      System.err.println(asmCode);
    }

    String sourceFilename = config.filename();
    File file = new File(dir, sourceFilename + ".asm");
    if (file.exists()) {
      file.delete();
    }
    try {
      file.createNewFile();

      CharSink charSink = Files.asCharSink(file, Charset.defaultCharset(), FileWriteMode.APPEND);
      charSink.writeLines(state.asmCode());

      ProcessBuilder pb = new ProcessBuilder("nasm", "-fwin64", file.getAbsolutePath());
      pb.directory(dir);
      Process process = pb.start();
      process.waitFor();

      if (process.exitValue() != 0) {
        InputStream stream1 = process.getErrorStream();
        String output = new String(ByteStreams.toByteArray(stream1));
        System.err.printf("%s error output: %s\n", "nasm", output);
        stream1 = process.getInputStream();
        output = new String(ByteStreams.toByteArray(stream1));
        System.err.printf("%s std output: %s\n", "nasm", output);
        assertWithMessage("nasm" + " had wrong exit value: " + output)
            .that(process.exitValue())
            .isEqualTo(0);
      }

      File obj = new File(dir, sourceFilename + ".obj");
      File exe = new File(dir, sourceFilename);
      pb = new ProcessBuilder("gcc", obj.getAbsolutePath(), "-o", exe.getAbsolutePath());
      pb.directory(dir);
      process = pb.start();
      process.waitFor();
      if (process.exitValue() != 0) {
        InputStream stream2 = process.getErrorStream();
        String output = new String(ByteStreams.toByteArray(stream2));
        System.err.printf("%s error output: %s\n", "Linking", output);
        stream2 = process.getInputStream();
        output = new String(ByteStreams.toByteArray(stream2));
        System.err.printf("%s std output: %s\n", "Linking", output);
        assertWithMessage("Linking" + " had wrong exit value: " + output)
            .that(process.exitValue())
            .isEqualTo(0);
      }

      pb = new ProcessBuilder(exe.getAbsolutePath());
      pb.directory(dir);
      process = pb.start();
      process.waitFor();
      InputStream stream = process.getInputStream();
      String compiledOutput = new String(ByteStreams.toByteArray(stream));
      state = state.addStdOut(compiledOutput);
      if (expectedRuntimeError.isPresent()) {
        System.out.println("COMPILED OUTPUT (hopefully with error):");
        System.out.println("------------------------------");
        System.out.println(compiledOutput);
        assertThat(compiledOutput).contains(expectedRuntimeError.get());
      } else {
        System.out.println("COMPILED OUTPUT:");
        System.out.println("------------------------------");
        System.out.println(compiledOutput);
      }

      int expectedExitCode = expectedRuntimeError.isPresent() ? -1 : 0;
      if (process.exitValue() != expectedExitCode) {
        InputStream errorStream = process.getErrorStream();
        String output = new String(ByteStreams.toByteArray(errorStream));
        System.err.printf("%s error output: %s\n", "Executable", output);
        errorStream = process.getInputStream();
        output = new String(ByteStreams.toByteArray(errorStream));
        System.err.printf("%s std output: %s\n", "Executable", output);
        assertWithMessage("Executable exit value of asm" + file)
            .that(process.exitValue())
            .isEqualTo(0);
      }
    } catch (IOException e) {
      e.printStackTrace();
      failWithActual("no exception", e);
    } catch (InterruptedException e) {
      e.printStackTrace();
      failWithActual("no exception", e);
    }
    return state;
  }
}
