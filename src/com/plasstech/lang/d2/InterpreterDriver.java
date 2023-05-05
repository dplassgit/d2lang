package com.plasstech.lang.d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.base.Joiner;
import com.google.devtools.common.options.OptionsParser;
import com.plasstech.lang.d2.common.D2Options;
import com.plasstech.lang.d2.interpreter.InterpreterResult;

public class InterpreterDriver {

  public static void main(String[] args) {
    OptionsParser parser = OptionsParser.newOptionsParser(D2Options.class);
    parser.parseAndExitUponError(args);
    D2Options options = parser.getOptions(D2Options.class);

    if (options.debuglex > 0) {
      System.out.printf("debugginglex %d\n", options.debuglex);
    }
    if (options.debugparse > 0) {
      System.out.printf("debugparse %d\n", options.debugparse);
    }
    if (options.debugtype > 0) {
      System.out.printf("debugtype %d\n", options.debugtype);
    }
    if (options.debugcodegen > 0) {
      System.out.printf("debugcodegen %d\n", options.debugcodegen);
    }
    if (options.debugopt > 0) {
      System.out.printf("debugopt %d\n", options.debugopt);
    }
    if (options.debugint > 0) {
      System.out.printf("debugint %d\n", options.debugint);
    }

    String filename = args[0];
    // 1. read file
    String text;
    try {
      text = new String(Files.readAllBytes(Paths.get(filename)));
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }

    InterpreterExecutor ee =
        new InterpreterExecutor(text)
            .setInteractive(true)
            .setLexDebugLevel(options.debuglex)
            .setParseDebugLevel(options.debugparse)
            .setTypeDebugLevel(options.debugtype)
            .setCodeGenDebugLevel(options.debugcodegen)
            .setOptDebugLevel(options.debugopt)
            .setIntDebugLevel(options.debugint)
            .setOptimize(options.optimize);

    InterpreterResult result = ee.execute();
    if (options.debugparse > 0) {
      System.out.println("\nPARSED PROGRAM:");
      System.out.println("------------------------------");
      System.out.println(ee.state().programNode());
    }

    if (options.debugint > 0) {
      System.out.println("\nSYMBOL TABLE:");
      System.out.println("------------------------------");
      System.out.println(result.symbolTable());
    }

    if (options.debugcodegen > 0) {
      System.out.println("\nFINAL INTERMEDIATE CODE:");
      System.out.println("------------------------------");
      System.out.println(Joiner.on("\n").join(result.code()));
    }

    if (options.debugint > 0) {
      // Since we're running in interactive mode, there's no need for this.
      System.out.println("\n------------------------------");
      System.out.printf("Cycles: %d\n", result.instructionCycles());
      System.out.printf("LOC: %d\n", result.linesOfCode());
      System.out.printf("Branches taken: %d\n", result.branchesTaken());
      System.out.printf("Branches not taken: %d\n", result.branchesNotTaken());
      System.out.printf("Gotos: %d\n", result.gotos());
      System.out.printf("Calls: %d\n", result.calls());
      System.out.println("Env:");
      System.out.println(result.environment());
      System.out.println("------------------------------");
      System.out.println("SYSTEM.OUT:");
      System.out.println("------------------------------");
      System.out.println(Joiner.on("").join(result.environment().output()));
    }
  }
}
