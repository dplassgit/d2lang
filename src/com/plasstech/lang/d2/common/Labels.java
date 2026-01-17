package com.plasstech.lang.d2.common;

/** Creates unique names and labels, using a sequence number. */
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

  /**
   * @deprecated: Use #generateLabel instead
   */
  @Deprecated
  public static String nextLabel(String prefix) {
    return singleton.generateLabel(prefix);
  }

  /**
   * @deprecated: Use #generateGlobal instead
   */
  @Deprecated
  public static String nextGlobal(String prefix) {
    return singleton.generateGlobal(prefix);
  }
}
