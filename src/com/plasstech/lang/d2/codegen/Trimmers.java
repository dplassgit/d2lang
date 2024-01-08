package com.plasstech.lang.d2.codegen;

import java.util.List;

import com.google.common.collect.ImmutableList;

public class Trimmers {
  /** Removes empty and lines that are only comments. Only right-trims. */
  public static ImmutableList<String> rightTrim(List<String> code) {
    return code.stream()
        .map(line -> trimComment(line))
        .filter(line -> !line.isEmpty())
        .collect(ImmutableList.toImmutableList());
  }

  /** Removes all comments from all lines, trims, then removes empty lines. */
  public static ImmutableList<String> trim(List<String> code) {
    return code.stream()
        .map(line -> trim(line))
        .filter(line -> !line.isEmpty())
        .collect(ImmutableList.toImmutableList());
  }

  /** Trim trailing comments and ALL leading and trailing whitespace. */
  public static String trim(String s) {
    return trimTrailingComment(s).trim();
  }

  /**
   * Trim trailing comment and trailing whitespace. If the line was only a comment, returns the
   * empty string.
   */
  public static String trimComment(String s) {
    s = rightTrim(trimTrailingComment(s));
    if (s.trim().isEmpty()) {
      return s.trim();
    }
    return s;
  }

  /** Trim a trailing comment from the line. Leaves other leading and trailing whitespace. */
  private static String trimTrailingComment(String s) {
    if (s.contains(" db ")) {
      return s;
    }
    int semi = s.indexOf(';');
    if (semi >= 0) {
      s = s.substring(0, semi);
    }
    return s;
  }

  /** Remove trailing whitespace only. */
  public static String rightTrim(String s) {
    while (s.endsWith(" ") || s.endsWith("\n")) {
      s = s.substring(0, s.length() - 1);
    }
    return s;
  }
}
