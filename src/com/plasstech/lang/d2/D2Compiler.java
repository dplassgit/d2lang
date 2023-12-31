package com.plasstech.lang.d2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.google.devtools.common.options.OptionsParser;
import com.plasstech.lang.d2.codegen.x64.NasmCodeGenerator;
import com.plasstech.lang.d2.codegen.x64.optimize.NasmOptimizer;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.common.D2Options;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.phase.State;

/**
 * Top-level driver for the compiler. See {@link D2Options} for options.
 */
public class D2Compiler {

  private static final String D2PATH = System.getenv("D2PATH");

  public static void main(String[] args) throws Exception {
    OptionsParser optionsParser = OptionsParser.newOptionsParser(D2Options.class);
    optionsParser.parseAndExitUponError(args);
    D2Options options = optionsParser.getOptions(D2Options.class);

    String sourceFilename = args[0];
    // 1. read file
    File sourceFile = new File(sourceFilename);
    CharSource charSource = Files.asCharSource(sourceFile, Charset.defaultCharset());
    String sourceCode = charSource.read();

    State state = compileToIntermediateLanguage(options, sourceCode);
    generateAsmAndLink(options, sourceFilename, state);
  }

  private static State compileToIntermediateLanguage(D2Options options, String sourceCode) {
    State state = null;
    try {
      YetAnotherCompiler yac = new YetAnotherCompiler();
      CompilationConfiguration config =
          CompilationConfiguration.builder()
              .setSourceCode(sourceCode)
              .setLexDebugLevel(options.debuglex)
              .setParseDebugLevel(options.debugparse)
              .setTypeDebugLevel(options.debugtype)
              .setCodeGenDebugLevel(options.debugcodegen)
              .setOptDebugLevel(options.debugopt)
              .setOptimize(options.optimize)
              .build();
      state = yac.compile(config);
    } catch (D2RuntimeException re) {
      if (state != null) {
        state.stopOnError(options.showStackTraces);
      }
    }
    if (state != null) {
      state.stopOnError(options.showStackTraces);
    }
    return state;
  }

  private static void generateAsmAndLink(D2Options options, String sourceFilename, State state)
      throws IOException, InterruptedException {
    switch (options.target) {
      case x64:
        state = new NasmCodeGenerator().execute(state);
        break;

      default:
        state =
            state.addException(
                new D2RuntimeException(
                    "Cannot generate asm for " + options.target + " yet", null, "D2 Compiler"));
        state.stopOnError(options.debugcodegen > 0 || options.showStackTraces);
        break;
    }
    if (options.debugcodegen > 0) {
      System.out.println("------------------------------");
      System.out.println("\nASM CODE:");
      System.out.println(Joiner.on("\n").join(state.asmCode()));
      System.out.println("------------------------------");
    }

    if (options.optimizeAsm) {
      state = new NasmOptimizer().execute(state);

      if (options.debugcodegen > 0) {
        System.out.println("------------------------------");
        System.out.println("\nOPTIMIZED ASM CODE:");
        System.out.println(Joiner.on("\n").join(state.asmCode()));
        System.out.println("------------------------------");
      }
    }

    File dir = new File(System.getProperty("user.dir"));
    String baseName = Files.getNameWithoutExtension(sourceFilename);
    File asmFile = new File(dir, baseName + "." + options.target.extension);
    if (asmFile.exists()) {
      asmFile.delete();
    }
    asmFile.createNewFile();

    CharSink charSink = Files.asCharSink(asmFile, Charset.defaultCharset(), FileWriteMode.APPEND);
    charSink.writeLines(state.asmCode(), "\n");

    // wait until now to throw the exception so that we've written the asm file.
    state.stopOnError(options.debugcodegen > 0 || options.showStackTraces);
    if (!options.compileOnly) {
      switch (options.target) {
        case x64:
          x64Assemble(options, dir, baseName, asmFile);
          break;

        default:
          state =
              state.addException(
                  new D2RuntimeException(
                      "Cannot assemble for " + options.target + " yet", null, "D2 Compiler"));
          break;
      }
    }
    if (!options.saveTemps) {
      asmFile.delete();
    }
    state.stopOnError(options.debugcodegen > 0 || options.showStackTraces);
  }

  private static void x64Assemble(D2Options options, File dir, String baseName, File asmFile)
      throws IOException, InterruptedException {

    File objFile = new File(dir, baseName + ".obj");
    if (objFile.exists()) {
      objFile.delete();
    }
    // Assemble
    ProcessBuilder pb =
        new ProcessBuilder(
            "nasm", "-fwin64", asmFile.getAbsolutePath(), "-o", objFile.getAbsolutePath());
    pb.directory(dir);
    if (options.showCommands) {
      System.out.println(Joiner.on(" ").join(pb.command()));
    }
    Process process = pb.start();
    process.waitFor();
    assertNoProcessError(process, "Assembling");

    if (!options.compileAndAssembleOnly) {
      File exeFile = new File(dir, options.exeName);
      if (exeFile.exists()) {
        exeFile.delete();
      }
      ImmutableList.Builder<String> command = new ImmutableList.Builder<String>().add("gcc",
          objFile.getAbsolutePath());
      if (options.libs != null && options.libs.size() > 0) {
        command.addAll(options.libs);
      }
      if (D2PATH != null) {
        String dlib = String.format("%s/dlib/dlib.obj", D2PATH);
        File dlibFile = new File(dlib);
        if (dlibFile.exists()) {
          command.add(dlib);
        }
      }
      command.add("-o", exeFile.getAbsolutePath());
      pb = new ProcessBuilder(command.build());
      pb.directory(dir);
      if (options.showCommands) {
        System.out.println(Joiner.on(" ").join(pb.command()));
      }
      process = pb.start();
      process.waitFor();
      assertNoProcessError(process, "Linking");
      if (!options.saveTemps) {
        objFile.delete();
      }
    }

  }

  private static void assertNoProcessError(Process process, String name) throws IOException {
    if (process.exitValue() != 0) {
      InputStream stream = process.getErrorStream();
      String output = new String(ByteStreams.toByteArray(stream));
      System.err.printf("%s output: %s\n", name, output);
      System.exit(process.exitValue());
    }
  }
}
