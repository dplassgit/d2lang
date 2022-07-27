package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Preconditions;

class StringTable extends ConstTable<String> {
  private final List<StringEntry> orderedEntries = new ArrayList<>();
  // Values sorted by descending length, with hash as tiebreak
  private final Set<String> values =
      new TreeSet<>(
          (a, b) -> {
            int lenDiff = b.length() - a.length();
            if (lenDiff != 0) {
              return lenDiff;
            }
            return b.hashCode() - a.hashCode();
          });
  private final Map<String, StringEntry> entries = new HashMap<>();
  private int index;

  @Override
  void add(String value) {
    orderedEntries.clear();
    values.add(value);
  }

  @Override
  List<? extends ConstEntry<String>> entries() {
    regenerateEntries();
    return orderedEntries;
  }

  private void regenerateEntries() {
    // 1. make simple entries for each value
    if (orderedEntries.isEmpty() && !values.isEmpty()) {
      for (String value : values) {
        // In order of size: largest to smallest
        orderedEntries.add(new StringConstant(generateName(value), value));
      }
      // 2. go from smallest to largest:
      for (int child = orderedEntries.size() - 1; child >= 0; child--) {
        StringEntry childEntry = orderedEntries.get(child);
        String childValue = childEntry.value();
        for (int parent = 0; parent < child; ++parent) {
          StringEntry parentEntry = orderedEntries.get(parent);
          //    a. go from largest to smallest to find a good "parent".
          if (parentEntry.value().endsWith(childValue)) {
            int offset = parentEntry.value().lastIndexOf(childValue);
            //    b. if found a good parent, replace child with relative
            RelativeStringConstant newChild =
                new RelativeStringConstant(childEntry.name(), parentEntry, offset);
            orderedEntries.set(child, newChild);
            break;
          }
        }
      }
      entries.clear();
      for (StringEntry entry : orderedEntries) {
        entries.put(entry.value(), entry);
      }
    }
  }

  private String generateName(String value) {
    StringBuilder sanitizedNameValue = new StringBuilder();
    // remove all non-alphanumerics
    for (char c : value.toCharArray()) {
      if (Character.isLetterOrDigit(c)) {
        sanitizedNameValue.append(c);
      }
    }
    return String.format("CONST_%s_%d", sanitizedNameValue, index++);
  }

  @Override
  ConstEntry<String> lookup(String value) {
    regenerateEntries();
    Preconditions.checkState(
        entries.containsKey(value), "Does not contain value %s: %s", value, orderedEntries);
    return entries.get(value);
  }

  int size() {
    regenerateEntries();
    return entries.size();
  }
}
