package com.plasstech.lang.d2.codegen;

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
public class GoldenTests {

  @Test
  public void lexerInDLexerInDGlobals() throws IOException {
    if (System.getenv("TEST_SRCDIR") == null) {
      String path = Paths.get("samples/d2ind2/lexerglobals.d").toString();
      String text = new String(Files.readAllBytes(Paths.get(path)));
      // replace "input" with the string itself.
      String quine = text.replace("text = input", String.format("text = \"%s\"", text));
      TestUtils.optimizeAssertSameVariables(quine);
    } else {
      // running in bazel
      fail("Sorry, cannot test in bazel");
    }
  }

  @Test
  public void lexerInDLexerInD() throws IOException {
    if (System.getenv("TEST_SRCDIR") == null) {
      String path = Paths.get("samples/d2ind2/lexer.d").toString();
      String text = new String(Files.readAllBytes(Paths.get(path)));
      // replace "input" with the string itself.
      String quine = text.replace("text = input", String.format("text = \"%s\"", text));
      TestUtils.optimizeAssertSameVariables(quine);
    } else {
      // running in bazel
      fail("Sorry, cannot test in bazel");
    }
  }

  @Test
  public void compileParserInD() throws IOException {
    if (System.getenv("TEST_SRCDIR") == null) {
      compileFile(Paths.get("samples/d2ind2/parser.d").toString());
    } else {
      // running in bazel
      fail("Sorry, cannot test in bazel");
    }
  }

  @Test
  public void compileAllNonGoldenSamples(
      @TestParameter(valuesProvider = NonGoldenFilesProvider.class) File file) throws IOException {
    if (System.getenv("TEST_SRCDIR") == null) {
      compileFile(file.getAbsolutePath());
    } else {
      // running in bazel
      fail("Sorry, cannot test in bazel");
    }
  }

  @Test
  public void runAllSamples(@TestParameter(valuesProvider = GoldenFilesProvider.class) File file)
      throws IOException {
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
    ImmutableList<Op> ilCode = TestUtils.compile(text).optimizedIlCode();
    System.out.println("\nOPTIMIZED:");
    System.out.println("------------------------------");
    System.out.println(Joiner.on("\n").join(ilCode));
    System.out.println();
  }

  private void testFromFile(String path) throws IOException {
    System.out.println("path = " + path);
    String text = new String(Files.readAllBytes(Paths.get(path)));
    TestUtils.optimizeAssertSameVariables(text);
  }
}
