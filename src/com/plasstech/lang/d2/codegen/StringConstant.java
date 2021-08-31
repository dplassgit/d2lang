package com.plasstech.lang.d2.codegen;

class StringConstant extends StringEntry {

  public StringConstant(String name, String value) {
    super(name, value);
  }

  @Override
  public String dataEntry() {
    // TODO: escape escape characters
    return String.format("%s: db \"%s\", 0", name(), value());
  }
}
