package com.plasstech.lang.d2.codegen.testing;

import static com.google.common.truth.Truth.assertWithMessage;

import java.io.IOException;
import java.io.InputStream;

import com.google.common.io.ByteStreams;

public class ProcessUtils {
  public static void assertNoProcessError(Process process, String name, int exitCode)
      throws IOException {
    if (process.exitValue() != exitCode) {
      InputStream stream = process.getErrorStream();
      String output = new String(ByteStreams.toByteArray(stream));
      System.err.printf("%s error output: %s\n", name, output);
      stream = process.getInputStream();
      output = new String(ByteStreams.toByteArray(stream));
      System.err.printf("%s std output: %s\n", name, output);
      assertWithMessage(name + " had wrong exit value: " + output)
          .that(process.exitValue())
          .isEqualTo(0);
    }
  }
}
