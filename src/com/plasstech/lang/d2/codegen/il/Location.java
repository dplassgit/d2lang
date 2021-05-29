package com.plasstech.lang.d2.codegen.il;

public abstract class Location implements Operand {
  private final String name;

  public Location() {
    this("none");
  }
  public Location(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return name;
  }
}
