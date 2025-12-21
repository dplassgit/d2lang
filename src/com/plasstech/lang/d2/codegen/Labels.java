package com.plasstech.lang.d2.codegen;

public class Labels {
  private static final Labels singleton = new Labels();

  public static Labels instance() {
    return singleton;
  }

  private int id;

  public String generateLabel(String prefix) {
    return generateGlobal("D_" + prefix);
  }

  public String generateGlobal(String prefix) {
    return String.format("%s_%d", prefix, id++);
  }

  public static String nextLabel(String prefix) {
    return singleton.generateLabel(prefix);
  }

  public static String nextGlobal(String prefix) {
    return singleton.generateGlobal(prefix);
  }
}
