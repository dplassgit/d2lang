package com.plasstech.lang.d2.common;

/**
 * Represents a location in the file: line and column.
 */
public class Position {
  private final int line;
  private final int column;

  public Position(int line, int column) {
    this.line = line;
    this.column = column;
  }

  public int line() {
    return line;
  }

  public int column() {
    return column;
  }

  @Override
  public String toString() {
    return String.format("%d,%d", line, column);
  }
}

