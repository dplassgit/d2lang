package com.plasstech.lang.d2.codegen;

import java.util.HashMap;
import java.util.Map;

import com.google.common.base.Preconditions;

class DoubleTable {
  private final Map<Double, String> entries = new HashMap<>();
  private int index;

  void addEntry(double value) {
    entries.put(value, generateName(value));
  }

  private String generateName(double value) {
    String valueAsString = Double.toString(value);
    StringBuilder sanitizedNameValue = new StringBuilder();
    // remove all non-alphanumerics
    for (char c : valueAsString.toCharArray()) {
      if (Character.isLetterOrDigit(c)) {
        sanitizedNameValue.append(c);
      } else if (c == '.') {
        sanitizedNameValue.append('_');
      }
    }
    return String.format("DOUBLE_%s_%d", sanitizedNameValue, index++);
  }

  String lookup(double value) {
    Preconditions.checkState(
        entries.containsKey(value), "Does not contain value %s: %s", value, entries);
    return entries.get(value);
  }
}
