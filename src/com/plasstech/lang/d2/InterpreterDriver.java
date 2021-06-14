package com.plasstech.lang.d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.interpreter.Environment;

public class InterpreterDriver {

  public static void main(String[] args) {
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
    Environment env = ee.execute();
    System.out.println("\nPARSED PROGRAM:");
    System.out.println(ee.programNode());

    System.out.println("\nSYMBOL TABLE:");
    System.out.println(ee.symbolTable());

    System.out.println("------------------------------");
    System.out.println("SYSTEM.OUT:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("").join(env.output()));
  }
}
