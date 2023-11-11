package com.plasstech.lang.d2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;

/**
 * To run:
 * 
 * <pre>
 * bazel run src/com/plasstech/lang/d2:CheckerDriver -- $PWD/samples/helloworld.d
 * </pre>
 */
public class CheckerDriver {
  public static void main(String[] args) {
    try {
      String filename = args[0];
      // read file
      String sourceCode = new String(Files.readAllBytes(Paths.get(filename)));
      YetAnotherCompiler yac = new YetAnotherCompiler();
      CompilationConfiguration config =
          CompilationConfiguration.builder().setSourceCode(sourceCode)
              .setTypeDebugLevel(2)
              .setLastPhase(PhaseName.TYPE_CHECK)
              .build();
      State state = yac.compile(config);
      ProgramNode node = state.programNode();
      System.out.println(node);
      state.stopOnError();
    } catch (IOException e) {
      e.printStackTrace();
      return;
    }
  }
}
