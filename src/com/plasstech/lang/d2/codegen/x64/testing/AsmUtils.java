package com.plasstech.lang.d2.codegen.x64.testing;

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.common.collect.ImmutableList;

public class AsmUtils {
  /** Trims all comments from the code, and trims each line. */
  public static ImmutableList<String> trimComments(ImmutableList<String> code) {
    return code.stream()
        .map(String::trim)
        .filter(s -> !s.startsWith(";"))
        .map(
            old -> {
              int semi = old.indexOf(';');
              if (semi != -1) {
                return old.substring(0, semi - 1);
              } else {
                return old;
              }
            })
        .map(String::trim)
        .collect(toImmutableList());
  }
}
