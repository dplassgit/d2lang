package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.parse.node.ProgramNode;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.TypeCheckResult;

/** NOTE: THESE TESTS CANNOT RUN FROM BAZEL */
public class GoldenTests {

  @Test
  public void lexerInD() throws IOException {
    if (System.getenv("TEST_SRCDIR") == null) {
      String path = Paths.get("samples/d2ind2/lexerglobals.d").toString();
      String text = new String(Files.readAllBytes(Paths.get(path)));
      // replace "input" with the string itself.
      String quine = text.replace("text = input", String.format("text = \"%s\"", text));
      TestUtils.optimizeAssertSameVariables(quine);
    } else {
      // running in blaze
      System.err.println("Sorry, cannot run from blaze");
    }
  }

  @Test
  public void typeCheckLexerInDWithRecord() throws IOException {
    if (System.getenv("TEST_SRCDIR") == null) {
      String path = Paths.get("samples/d2ind2/lexer.d").toString();
      String text = new String(Files.readAllBytes(Paths.get(path)));
      Lexer lex = new Lexer(text);
      Parser parser = new Parser(lex);
      Node node = parser.parse();
      assertThat(node.isError()).isFalse();

      StaticChecker checker = new StaticChecker((ProgramNode) node);
      TypeCheckResult typeCheckResult = checker.execute();
      if (typeCheckResult.isError()) {
        fail(typeCheckResult.message());
      }
      System.out.println(typeCheckResult.symbolTable());
    } else {
      // running in blaze
      System.err.println("Sorry, cannot run from blaze");
    }
  }

  @Test
  public void testAllSamples() throws Exception {
    if (System.getenv("TEST_SRCDIR") == null) {
      List<File> files =
          Files.list(Paths.get("samples"))
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".d"))
              .map(Path::toFile)
              .collect(Collectors.toList());
      for (File file : files) {
        testFromFile(file.getAbsolutePath());
      }
    } else {
      // running in blaze
      System.err.println("Sorry, cannot run from blaze");
    }
  }

  private void testFromFile(String path) throws IOException {
    System.out.println("path = " + path);
    String text = new String(Files.readAllBytes(Paths.get(path)));
    TestUtils.optimizeAssertSameVariables(text);
  }
}
