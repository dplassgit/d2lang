package com.plasstech.lang.d2.common;

/** A constant range object */
public record Range(int start, int end) {

  public long value() {
    return ((long) start() << 32) + end();
  }

  public int value(int index) {
    if (index == 0) {
      return start();
    }
    return end();
  }
}
