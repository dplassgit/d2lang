package com.plasstech.lang.d2.common;

/** Represents a location in the file: line and column. */
public record Position(int line, int column) {
  @Override
  public String toString() {
    return String.format("line %d, column %d", line(), column());
  }
}
