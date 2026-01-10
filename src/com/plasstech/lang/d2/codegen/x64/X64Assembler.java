package com.plasstech.lang.d2.codegen.x64;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.io.ByteStreams;
import com.plasstech.lang.d2.codegen.asm.Assembler;
import com.plasstech.lang.d2.common.D2Options;

public class X64Assembler implements Assembler {

  private final String d2Path;

  public X64Assembler(String d2Path) {
    this.d2Path = d2Path;
  }

  @Override
  public File assemble(D2Options options, String baseName, File asmFile)
      throws IOException, InterruptedException {

    File dir = asmFile.getParentFile();
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
      ImmutableList.Builder<String> command =
          new ImmutableList.Builder<String>().add("gcc", "-s", objFile.getAbsolutePath());
      if (options.libs != null && options.libs.size() > 0) {
        command.addAll(options.libs);
      }
      if (d2Path != null) {
        String dlib = String.format("%s/dlib/dlib.obj", d2Path);
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
      return exeFile;
    }
    return null;
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
