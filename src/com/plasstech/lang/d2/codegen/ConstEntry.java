package com.plasstech.lang.d2.codegen;

/** Represents an abstract constant in the nasm data section. */
public abstract class ConstEntry<T> {
  private final T value;
  private final String name;

  ConstEntry(String name, T value) {
    this.name = name;
    this.value = value;
  }

  public T value() {
    return value;
  }

  public String name() {
    return name;
  }

  public abstract String dataEntry();
}
