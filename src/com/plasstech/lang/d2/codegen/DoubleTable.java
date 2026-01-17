package com.plasstech.lang.d2.codegen;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.common.Labels;

/** Stores a table of DoubleEntry values, mapped by... double value. */
public class DoubleTable implements ConstTable<Double> {
  private final Map<Double, DoubleEntry> entries = new HashMap<>();

  @Override
  public void add(Double value) {
    Preconditions.checkNotNull(value, "Double value cannot be null");
    entries.put(value, new DoubleEntry(generateName(value), value));
  }

  @Override
  public List<? extends ConstEntry<Double>> entries() {
    return ImmutableList.copyOf(entries.values());
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
    return Labels.nextGlobal(String.format("DOUBLE_%s", sanitizedNameValue));
  }

  @Override
  public ConstEntry<Double> lookup(Double value) {
    Preconditions.checkState(
        entries.containsKey(value), "Does not contain value %s: %s", value, entries);
    return entries.get(value);
  }
}
