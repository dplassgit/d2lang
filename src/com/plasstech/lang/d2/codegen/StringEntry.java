package com.plasstech.lang.d2.codegen;

abstract class StringEntry {

  private final String name;
  private final String value;

  StringEntry(String name, String value) {
    this.name = name;
    this.value = value;
  }

  abstract String dataEntry();

  @Override
  public String toString() {
    return dataEntry();
  }

  String name() {
    return name;
  }

  String value() {
    return value;
  }
}
