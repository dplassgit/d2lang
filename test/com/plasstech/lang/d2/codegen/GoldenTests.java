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

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.lex.Lexer;
import com.plasstech.lang.d2.parse.Parser;
import com.plasstech.lang.d2.parse.node.Node;
import com.plasstech.lang.d2.type.StaticChecker;
import com.plasstech.lang.d2.type.SymTab;
import com.plasstech.lang.d2.type.TypeCheckResult;

/** NOTE: THESE TESTS CANNOT RUN FROM BAZEL */
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
      // running in blaze
      System.err.println("Sorry, cannot run from blaze");
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
      // running in blaze
      System.err.println("Sorry, cannot run from blaze");
    }
  }

  @Test
  public void compileParserInD() throws IOException {
    if (System.getenv("TEST_SRCDIR") == null) {
      compileFile(Paths.get("samples/d2ind2/parser.d").toString());
    } else {
      // running in blaze
      System.err.println("Sorry, cannot run from blaze");
    }
  }

  @Test
  public void compileAllNonGoldenSamples() throws IOException {
    if (System.getenv("TEST_SRCDIR") == null) {
      List<File> files =
          Files.list(Paths.get("samples/non-golden"))
              .filter(Files::isRegularFile)
              .filter(path -> path.toString().endsWith(".d"))
              .map(Path::toFile)
              .collect(Collectors.toList());
      for (File file : files) {
        compileFile(file.getAbsolutePath());
      }
    } else {
      // running in blaze
      System.err.println("Sorry, cannot run from blaze");
    }
  }

  @Test
  public void runAllSamples() throws Exception {
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

  private void compileFile(String path) throws IOException {
    System.out.println("path = " + path);
    String text = new String(Files.readAllBytes(Paths.get(path)));
    Lexer lex = new Lexer(text);
    Parser parser = new Parser(lex);
    Node node = parser.parse();
    if (node.isError()) {
      fail(node.message());
    }

    StaticChecker checker = new StaticChecker(node);
    TypeCheckResult typeCheckResult = checker.execute();
    if (typeCheckResult.isError()) {
      fail(typeCheckResult.message());
    }
    SymTab symbolTable = typeCheckResult.symbolTable();

    CodeGenerator<Op> codegen = new ILCodeGenerator(node, symbolTable);
    ImmutableList<Op> ilCode = codegen.generate();
    // Runs all the optimizers.
    ILOptimizer optimizer = new ILOptimizer(2);
    ilCode = optimizer.optimize(ilCode);
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
