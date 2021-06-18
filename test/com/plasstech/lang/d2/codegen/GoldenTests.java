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

/** NOTE: THESE TESTS CANNOT RUN FROM BLAZE */
public class GoldenTests {

  @Test
  public void lexerInD() {
    if (System.getenv("TEST_SRCDIR") == null) {
      String path = Paths.get("samples/d2ind2/lexerglobals.d").toString();
      testFromFile(path);
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

  private void testFromFile(String path) {
    try {
      System.out.println("path = " + path);
      String text = new String(Files.readAllBytes(Paths.get(path)));
      TestUtils.optimizeAssertSameVariables(text);
    } catch (IOException e) {
      e.printStackTrace();
      fail("Could not read " + path);
    } catch (RuntimeException e) {
      e.printStackTrace();
      fail("Could not compile " + path + " " + e.getMessage());
    }
  }
}
