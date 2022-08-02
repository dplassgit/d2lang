package com.plasstech.lang.d2.codegen;

/** Represents a double constant in the nasm data section. */
class DoubleEntry extends ConstEntry<Double> {

  DoubleEntry(String name, double value) {
    super(name, value);
  }

  @Override
  String dataEntry() {
    return String.format("%s: dq %f", name(), value());
  }
}
