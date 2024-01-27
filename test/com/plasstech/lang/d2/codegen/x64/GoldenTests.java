package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.google.testing.junit.testparameterinjector.TestParameterValuesProvider;
import com.plasstech.lang.d2.YetAnotherCompiler;
import com.plasstech.lang.d2.common.CompilationConfiguration;
import com.plasstech.lang.d2.phase.State;

@RunWith(TestParameterInjector.class)
public class GoldenTests {

  @TestParameter
  boolean optimize;

  @Before
  public void setUp() {
    /** NOTE: THESE TESTS CANNOT BE RUN BY BAZEL */
    assumeTrue(System.getenv("TEST_SRCDIR") == null);
  }

  @Test
  public void compileNonGoldenSample(
      @TestParameter(valuesProvider = NonGoldenFilesProvider.class) File file) throws IOException {
    compileOneFile(file);
  }

  @Test
  public void compileBootstrap() throws IOException {
    compileOneFile(new File("src/bootstrap/v0/v0.d"));
  }

  @Test
  public void compileGames() throws IOException {
    compileOneFile(new File("samples/games/ge.d"));
  }

  // Just compile, no running
  private void compileOneFile(File file, boolean goldenOptimize) throws IOException {
    compileFile(file.getAbsolutePath(), goldenOptimize);
  }

  @Test
  public void testSample(@TestParameter(valuesProvider = GoldenFilesProvider.class) File file)
      throws Exception {
    testFromFile(file.getAbsolutePath());
  }

  private abstract static class FilesProvider extends TestParameterValuesProvider {
    private final String directory;

    FilesProvider(String directory) {
      this.directory = directory;
    }

    @Override
    public List<File> provideValues(Context context) {
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

  private void compileFile(String path) throws IOException {
    // System.out.println("path = " + path);
    String text = new String(Files.readAllBytes(Paths.get(path)));

    CompilationConfiguration config =
        CompilationConfiguration.builder()
            .setSourceCode(text)
            .setFilename(path)
            .setOptimize(optimize)
            .setCodeGenDebugLevel(0)
            .setOptDebugLevel(0)
            .build();
    State result = new YetAnotherCompiler().compile(config);
    if (result.error()) {
      fail(result.errorMessage());
    }
  }

  private void testFromFile(String path) throws Exception {
    System.out.println("path = " + path);
    String text = new String(Files.readAllBytes(Paths.get(path)));
    assertThatCompiling(text)
        .withOptimize(optimize)
        .withCodeGenDebugLevel(2)
        .withOptDebugLevel(0)
        .executedEqualsInterpreted();
  }
}
