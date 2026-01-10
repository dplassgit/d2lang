package com.plasstech.lang.d2;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import com.google.common.base.Joiner;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.google.devtools.common.options.OptionsParser;
import com.plasstech.lang.d2.codegen.x64.NasmCodeGenerator;
import com.plasstech.lang.d2.codegen.x64.X64Assembler;
import com.plasstech.lang.d2.codegen.x64.optimize.NasmOptimizer;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.common.D2Options;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.phase.State;

/** Top-level driver for the compiler. See {@link D2Options} for options. */
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

  // TODO: move this to YetAnotherCompiler, and re-use in ExecutionSubject
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
              .setRuntimeChecks(options.runtimeChecks)
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
        // TODO: move this to another place
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
      switch (options.target) {
        case x64:
          // TODO: move this to another place
          state = new NasmOptimizer(options.debugopt).execute(state);
          break;

        default:
          state =
              state.addException(
                  new D2RuntimeException(
                      "Cannot optimize asm for " + options.target + " yet", null, "D2 Compiler"));
          state.stopOnError(options.debugopt > 0 || options.showStackTraces);
          break;
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

    // Wait until now to throw the exception so that we've written the asm file.
    state.stopOnError(options.debugcodegen > 0 || options.showStackTraces);
    if (!options.compileOnly) {
      switch (options.target) {
        case x64:
          new X64Assembler(D2PATH).assemble(options, baseName, asmFile);
          break;

        default:
          state =
              state.addException(
                  new D2RuntimeException(
                      "Cannot assemble for " + options.target + " yet", null, "D2 Compiler"));
          state.stopOnError(options.debugcodegen > 0 || options.showStackTraces);
          break;
      }
    }
    if (!options.saveTemps) {
      asmFile.delete();
    }
    state.stopOnError(options.debugcodegen > 0 || options.showStackTraces);
  }
}
