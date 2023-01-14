package com.plasstech.lang.d2.codegen;

/** Represents a string constant that is defined relative to another string constant. */
class RelativeStringConstant extends StringEntry {

  private final int offset;
  private final String base;

  RelativeStringConstant(String name, StringEntry source, int offset) {
    super(name, source.value().substring(offset));
    this.offset = offset;
    this.base = source.name();
  }

  int offset() {
    return offset;
  }

  String base() {
    return base;
  }

  @Override
  public String dataEntry() {
    // This works in both nasm and the t100 assembler
    return String.format("%s EQU %s+0x%02x", name(), base(), offset());
  }
}
