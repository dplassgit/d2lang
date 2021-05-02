package com.plasstech.lang.d2.common;

public class Position {
  private final int line;
  private final int col;

  public Position(int line, int col) {
    this.line = line;
    this.col = col;
  }

  public int getLine() {
    return line;
  }

  public int getCol() {
    return col;
  }

  @Override
  public String toString() {
    return String.format("%d,%d", line, col);
  }
}

