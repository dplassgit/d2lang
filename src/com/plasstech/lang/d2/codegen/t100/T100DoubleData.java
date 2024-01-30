package com.plasstech.lang.d2.codegen.t100;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.codegen.ConstEntry;

class T100DoubleData extends ConstEntry<Double> {

  // I don't love this
  T100DoubleData(ConstEntry<Double> backing) {
    super(backing.name(), backing.value());
  }

  T100DoubleData(String name, double value) {
    super(name, value);
  }

  @Override
  public String dataEntry() {
    List<String> hexValues = new ArrayList<>();
    int[] bytes = Doubles.toT100Format(value());
    for (int b : bytes) {
      hexValues.add(String.format("0x%02x", b));
    }
    return String.format("%s: db %s", name(), Joiner.on(',').join(hexValues));
  }
}
