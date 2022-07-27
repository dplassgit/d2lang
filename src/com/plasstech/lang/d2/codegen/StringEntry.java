package com.plasstech.lang.d2.codegen;

/** Represents a string constant in the data section of the nasm file. */
abstract class StringEntry extends ConstEntry<String> {
  StringEntry(String name, String value) {
    super(name, value);
  }

  @Override
  public String toString() {
    return dataEntry();
  }
}
