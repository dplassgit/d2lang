package com.plasstech.lang.d2.phase;

/** Represents a compiler phase. */
public interface Phase {
  enum PhaseName {
    LEX,
    PARSE,
    TYPE_CHECK,
    IL_CODEGEN,
    IL_OPTIMIZE,
    ASM_CODGEN,
    ASM_OPTIMIZE,
  }

  State execute(State input);
}
