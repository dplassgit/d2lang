package com.plasstech.lang.d2.phase;

public enum PhaseName {
  PHASE_UNDEFINED,
  LEX,
  PARSE,
  TYPE_CHECK,
  IL_CODEGEN,
  IL_OPTIMIZE,
  ASM_CODEGEN,
  ASM_OPTIMIZE,
}