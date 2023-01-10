package com.plasstech.lang.d2.common;

/** Represents the current target architecture. */
public enum Target {
  x64("asm"),
  mos6502("asm"),
  i8085("as"),
  java("java"), // it could happen.
  c("c");

  public final String extension;

  Target(String extension) {
    this.extension = extension;
  }
}
