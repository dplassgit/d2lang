package com.plasstech.lang.d2.common;

import com.google.auto.value.AutoValue;

@AutoValue
/** A constant range object */
public abstract class Range {
  public abstract int start();

  public abstract int end();

  public long value() {
    return ((long) start() << 32) + end();
  }

  public int value(int index) {
    if (index == 0) {
      return start();
    }
    return end();
  }

  public static Range create(int start, int end) {
    return new AutoValue_Range(start, end);
  }
}
