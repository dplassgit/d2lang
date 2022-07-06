package com.plasstech.lang.d2;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;

import com.google.common.base.Joiner;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharSink;
import com.google.common.io.CharSource;
import com.google.common.io.FileWriteMode;
import com.google.common.io.Files;
import com.google.devtools.common.options.OptionsParser;
import com.plasstech.lang.d2.codegen.ILCodeGenerator;
import com.plasstech.lang.d2.codegen.NasmCodeGenerator;
import com.plasstech.lang.d2.common.D2Options;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.optimize.ILOptimizer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.phase.State;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.SymTab;

public class D2Compiler {

  public static void main(String[] args) throws Exception {
    OptionsParser optionsParser = OptionsParser.newOptionsParser(D2Options.class);
    optionsParser.parseAndExitUponError(args);
    D2Options options = optionsParser.getOptions(D2Options.class);

    String sourceFilename = args[0];
    // 1. read file
    File sourceFile = new File(sourceFilename);
    CharSource charSource = Files.asCharSource(sourceFile, Charset.defaultCharset());
    String sourceCode = charSource.read();

    State state = State.create(sourceCode).build();
    state = state.addFilename(sourceFilename);
    Lexer lexer = new Lexer(state.sourceCode());
    Parser parser = new Parser(lexer);
    state = parser.execute(state);
    if (state.error()) {
      throw state.exception();
    }
    if (options.debugparse > 0) {
      System.out.println("------------------------------");
      System.out.println("\nPARSED PROGRAM:");
      System.out.println(state.programNode());
      System.out.println("------------------------------");
    }

    StaticChecker checker = new StaticChecker();
    state = checker.execute(state);
    if (state.error()) {
      throw state.exception();
    }

    SymTab symbolTable = state.symbolTable();
    if (options.debugint > 0) {
      System.out.println("------------------------------");
      System.out.println("\nSYMBOL TABLE:");
      System.out.println(symbolTable);
      System.out.println("------------------------------");
    }

    ILCodeGenerator codegen = new ILCodeGenerator();
    state = codegen.execute(state);
    if (options.debugcodegen > 0) {
      System.out.println("------------------------------");
      System.out.println("\nINITIAL INTERMEDIATE CODE:");
      System.out.println(Joiner.on("\n").join(state.ilCode()));
      System.out.println("------------------------------");
    }
    if (options.optimize) {
      // Runs all the optimizers.
      ILOptimizer optimizer = new ILOptimizer(options.debugopt);
      state = optimizer.execute(state);
      if (options.debugcodegen > 0) {
        System.out.println("------------------------------");
        System.out.println("\nFINAL INTERMEDIATE CODE:");
        System.out.println(Joiner.on("\n").join(state.optimizedIlCode()));
        System.out.println("------------------------------");
      }
    }

    state = new NasmCodeGenerator().execute(state);
    if (options.debugcodegen > 0) {
      System.out.println("------------------------------");
      System.out.println("\nASM CODE:");
      System.out.println(Joiner.on("\n").join(state.asmCode()));
      System.out.println("------------------------------");
    }

    File dir = new File(System.getProperty("user.dir"));
    String baseName = Files.getNameWithoutExtension(sourceFilename);
    File asmFile = new File(dir, baseName + ".asm");
    if (asmFile.exists()) {
      asmFile.delete();
    }
    asmFile.createNewFile();

    CharSink charSink = Files.asCharSink(asmFile, Charset.defaultCharset(), FileWriteMode.APPEND);
    charSink.writeLines(state.asmCode(), "\n");

    // wait until now to throw the exception so that we've written the asm file.
    if (state.error()) {
      throw state.exception();
    }
    if (!options.compileOnly) {
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
        pb = new ProcessBuilder("gcc", objFile.getAbsolutePath(), "-o", exeFile.getAbsolutePath());
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
      if (!options.saveTemps) {
        asmFile.delete();
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
