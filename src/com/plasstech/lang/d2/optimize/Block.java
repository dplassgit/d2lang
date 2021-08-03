package com.plasstech.lang.d2.optimize;

/** Represents the start and end IP of a block. It is MUTABLE. */
class Block {
  private int start;
  private int end;

  Block(int start, int end) {
    this.start = start;
    this.end = end;
  }

  int start() {
    return start;
  }

  void setStart(int start) {
    this.start = start;
  }

  int end() {
    return end;
  }

  void setEnd(int end) {
    this.end = end;
  }

  @Override
  public String toString() {
    return String.format("ip %d to %d", start, end);
  }
}
