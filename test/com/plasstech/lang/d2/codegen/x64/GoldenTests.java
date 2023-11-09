package com.plasstech.lang.d2.codegen.x64;

import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameter.TestParameterValuesProvider;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.YetAnotherCompiler;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.phase.State;

/** NOTE: THESE TESTS CANNOT BE RUN BY BAZEL */
@RunWith(TestParameterInjector.class)
public class GoldenTests extends NasmCodeGeneratorTestBase {

  @Test
  public void compileNonGoldenSample(
      @TestParameter(valuesProvider = NonGoldenFilesProvider.class) File file,
      @TestParameter boolean goldenOptimize) throws IOException {
    compileOneFile(file, goldenOptimize);
  }

  @Test
  public void compileBootstrap(@TestParameter boolean goldenOptimize) throws IOException {
    compileOneFile(new File("src/bootstrap/v0/v0.d"), goldenOptimize);
  }

  @Test
  public void compileGames(@TestParameter boolean goldenOptimize) throws IOException {
    compileOneFile(new File("samples/games/ge.d"), goldenOptimize);
  }

  // Just compile, no running
  private void compileOneFile(File file, boolean goldenOptimize) throws IOException {
    if (System.getenv("TEST_SRCDIR") == null) {
      compileFile(file.getAbsolutePath(), goldenOptimize);
    } else {
      // running in bazel
      fail("Sorry, cannot test in bazel");
    }
  }

  @Test
  public void testSample(@TestParameter(valuesProvider = GoldenFilesProvider.class) File file)
      throws Exception {
    if (System.getenv("TEST_SRCDIR") == null) {
      testFromFile(file.getAbsolutePath());
    } else {
      // running in bazel
      fail("Sorry, cannot test in bazel");
    }
  }

  private abstract static class FilesProvider implements TestParameterValuesProvider {
    private final String directory;

    FilesProvider(String directory) {
      this.directory = directory;
    }

    @Override
    public List<File> provideValues() {
      try {
        return Files.list(Paths.get(directory))
            .filter(Files::isRegularFile)
            .filter(path -> path.toString().endsWith(".d"))
            .map(Path::toFile)
            .collect(Collectors.toList());
      } catch (IOException e) {
        fail(e.getMessage());
        return null;
      }
    }
  }

  private static class NonGoldenFilesProvider extends FilesProvider {
    NonGoldenFilesProvider() {
      super("samples/non-golden");
    }
  }

  private static class GoldenFilesProvider extends FilesProvider {
    GoldenFilesProvider() {
      super("samples");
    }
  }

  private void compileFile(String path, boolean goldenOptimize) throws IOException {
    System.out.println("path = " + path);
    String text = new String(Files.readAllBytes(Paths.get(path)));

    CompilationConfiguration config =
        CompilationConfiguration.builder()
            .setSourceCode(text)
            .setFilename(path)
            .setOptimize(goldenOptimize)
            .setCodeGenDebugLevel(0)
            .setOptDebugLevel(0)
            .build();
    State state = new YetAnotherCompiler().compile(config);
    ImmutableList<Op> ilCode = state.lastIlCode();

    System.out.println("\nCODE:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("\n").join(ilCode));
    System.out.println();
  }

  private void testFromFile(String path) throws Exception {
    System.out.println("path = " + path);
    String text = new String(Files.readAllBytes(Paths.get(path)));
    execute(text, "golden");
  }
}
