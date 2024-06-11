package com.plasstech.lang.d2.common;

import com.google.auto.value.AutoValue;

/** Represents a location in the file: line and column. */
@AutoValue
public abstract class Position {
  public static Position create(int line, int column) {
    return new AutoValue_Position(line, column);
  }

  public abstract int line();

  public abstract int column();

  @Override
  public String toString() {
    return String.format("line %d, column %d", line(), column());
  }
}
