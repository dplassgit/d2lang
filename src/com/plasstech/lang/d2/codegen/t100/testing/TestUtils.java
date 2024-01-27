package com.plasstech.lang.d2.codegen.t100.testing;

import static com.google.common.truth.Truth.assertThat;

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
import com.plasstech.lang.d2.codegen.t100.T100CodeGenerator;
import com.plasstech.lang.d2.codegen.testing.ProcessUtils;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.phase.Phase;
import com.plasstech.lang.d2.phase.PhaseName;
import com.plasstech.lang.d2.phase.State;

public class TestUtils {

  private static File dir = createTempDir();

  private static File createTempDir() {
    try {
      return java.nio.file.Files.createTempDirectory("tmp").toFile();
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static void assertInterpretedEqualsEmulated(String source) throws Exception {
    InterpreterResult result = interpret(source);
    String interpretedOutput =
        Joiner.on("").join(result.environment().output()).replaceAll("\n", "\r\n");

    State state = compile(result.state());
    assertThat(interpretedOutput).isEqualTo(state.stdOut());
  }

  // ugh, this really should be ASSEMBLERPATH
  private static final String ASSEMBLER_PATH = System.getenv("PYTHONPATH");

  private static State compile(State state) throws Exception {
    Phase codeGen = new T100CodeGenerator();
    state = codeGen.execute(state);
    state.throwOnError();

    String sourceFilename = "test";
    File file = new File(dir, sourceFilename + ".as");
    if (file.exists()) {
      file.delete();
    }
    file.createNewFile();

    File assembled = new File(dir, sourceFilename + ".as.8085");
    if (assembled.exists()) {
      assembled.delete();
    }
    assembled.createNewFile();

    CharSink charSink = Files.asCharSink(file, Charset.defaultCharset(), FileWriteMode.APPEND);
    charSink.writeLines(state.asmCode());

    ProcessBuilder pb =
        new ProcessBuilder("python", ASSEMBLER_PATH + "/assembler.py", file.getAbsolutePath(),
            "-s");
    pb.directory(dir);
    pb.redirectOutput(assembled);
    Process process = pb.start();
    process.waitFor();
    ProcessUtils.assertNoProcessError(process, "python", 0);

    pb = new ProcessBuilder("src/8085_emu/cpu.exe");
    pb.redirectInput(assembled);
    pb.directory(dir);
    process = pb.start();
    process.waitFor();
    InputStream stream = process.getInputStream();
    ProcessUtils.assertNoProcessError(process, "emulator", 0);

    String compiledOutput = new String(ByteStreams.toByteArray(stream));
    state = state.addStdOut(compiledOutput);

    return state;
  }

  private static InterpreterResult interpret(String source) {
    CompilationConfiguration config =
        CompilationConfiguration.builder()
            .setSourceCode(source)
            .setOptimize(true)
            .setLastPhase(PhaseName.IL_OPTIMIZE)
            .build();
    InterpreterExecutor executor = new InterpreterExecutor(config);
    executor.state().throwOnError();
    InterpreterResult interpreterResult = executor.execute();
    System.err.println(Joiner.on('\n').join(executor.state().ilCode()));
    System.err.println(Joiner.on("").join(interpreterResult.environment().output()));
    return interpreterResult;
  }
}
