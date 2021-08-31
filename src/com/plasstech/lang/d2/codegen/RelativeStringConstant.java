package com.plasstech.lang.d2.codegen;

class RelativeStringConstant extends StringEntry {

  private final int offset;
  private final String base;

  RelativeStringConstant(String name, StringEntry source, int offset) {
    super(name, source.value().substring(offset));
    this.offset = offset;
    this.base = source.name();
  }

  public int offset() {
    return offset;
  }

  public String base() {
    return base;
  }

  @Override
  public String dataEntry() {
    return String.format("%s EQU %s+%d", name(), base(), offset());
  }
}
