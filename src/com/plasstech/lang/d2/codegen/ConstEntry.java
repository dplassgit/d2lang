package com.plasstech.lang.d2.codegen;

/** Represents an abstract constant in the nasm data section. */
abstract class ConstEntry<T> {
  private final T value;
  private final String name;

  ConstEntry(String name, T value) {
    this.name = name;
    this.value = value;
  }
  
  T value() {
    return value;
  }

  String name() {
    return name;
  }

  abstract String dataEntry();
}
