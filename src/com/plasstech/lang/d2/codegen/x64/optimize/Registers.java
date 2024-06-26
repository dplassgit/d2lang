package com.plasstech.lang.d2.codegen.x64.optimize;

final class Registers {
  static final String BIG4_REGS = "[RE]?[A-D][XLH]";
  static final String INDEX_REGS = "[RE]?[SD]IL?";
  static final String EXT_REGS = "[Rr][0-9][0-5]?[bwd]?";
  static final String REGISTER = String.format("%s|%s|%s", BIG4_REGS, INDEX_REGS, EXT_REGS);
}
