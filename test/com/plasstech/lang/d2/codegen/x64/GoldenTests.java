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
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.testing.TestUtils;

/** NOTE: THESE TESTS CANNOT BE RUN BY BAZEL */
@RunWith(TestParameterInjector.class)
public class GoldenTests extends NasmCodeGeneratorTestBase {

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
  private void compileOneFile(File file) throws IOException {
    if (System.getenv("TEST_SRCDIR") == null) {
      compileFile(file.getAbsolutePath());
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

  private void compileFile(String path) throws IOException {
    System.out.println("path = " + path);
    String text = new String(Files.readAllBytes(Paths.get(path)));
    // ohcrap this isn't actually compiling without the optimizer... 
    ImmutableList<Op> ilCode = TestUtils.compile(text).optimizedIlCode();
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
