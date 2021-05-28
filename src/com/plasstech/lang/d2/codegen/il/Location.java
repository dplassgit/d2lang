package com.plasstech.lang.d2.codegen.il;

public abstract class Location {
  private final String name;

  public Location(String name) {
    this.name = name;
  }

  public String name() {
    return name;
  }

  @Override
  public String toString() {
    return String.format("%s (%s)", name, this.getClass().getSimpleName());
  }
}
