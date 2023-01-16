package com.plasstech.lang.d2.codegen;

public class Labels {
  private static int id;

  public static String nextLabel(String prefix) {
    return String.format("__%s_%d", prefix, id++);
  }
}
