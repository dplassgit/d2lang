package com.plasstech.lang.d2.phase;

/** Represents a compiler phase. */
public interface Phase {
  State execute(State input);
}
