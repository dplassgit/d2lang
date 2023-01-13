package com.plasstech.lang.d2.common;

/** Represents the current target architecture. */
public enum Target {
  x64("asm", 8),
  t100("as", 2); // Tandy 100/102, running the Intel 8085 processor

  public final String extension;
  public final int pointerSize;

  Target(String extension, int size) {
    this.extension = extension;
    this.pointerSize = size;
  }

  // I don't love this (having a static "singleton".)
  private static Target currentTarget = x64;

  public static void setTarget(Target target) {
    currentTarget = target;
  }

  public static Target target() {
    return currentTarget;
  }
}
