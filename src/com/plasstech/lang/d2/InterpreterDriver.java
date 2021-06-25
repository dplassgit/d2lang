package com.plasstech.lang.d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.base.Joiner;
import com.google.devtools.common.options.OptionsParser;
import com.plasstech.lang.d2.common.D2Options;
import com.plasstech.lang.d2.interpreter.Environment;

public class InterpreterDriver {

  public static void main(String[] args) {
    OptionsParser parser = OptionsParser.newOptionsParser(D2Options.class);
    parser.parseAndExitUponError(args);
    D2Options options = parser.getOptions(D2Options.class);

    if (options.debuglex > 0) {
      System.out.printf("Debugging lex %d\n", options.debuglex);
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
    ExecutionEnvironment ee = new ExecutionEnvironment(text, true);
    ee.setLexDebugLevel(options.debuglex);
    ee.setParseDebugLevel(options.debugparse);
    ee.setTypeDebugLevel(options.debugtype);
    ee.setCodeGenDebugLevel(options.debugcodegen);
    ee.setOptDebugLevel(options.debugopt);

    Environment env = ee.execute();
    if (options.debugparse > 0) {
      System.out.println("\nPARSED PROGRAM:");
      System.out.println("------------------------------");
      System.out.println(ee.programNode());
    }

    if (options.debugint > 0) {
      System.out.println("\nSYMBOL TABLE:");
      System.out.println("------------------------------");
      System.out.println(ee.symbolTable());
    }

    if (options.debugcodegen > 0) {
      System.out.println("\nFINAL INTERMEDIATE CODE:");
      System.out.println("------------------------------");
      System.out.println(Joiner.on("\n").join(ee.ilCode()));
    }
    if (options.debugint > 0) {
      System.out.println("\n------------------------------");
      System.out.println("SYSTEM.OUT:");
      System.out.println("------------------------------");
    }
    System.out.println(Joiner.on("").join(env.output()));
  }
}
