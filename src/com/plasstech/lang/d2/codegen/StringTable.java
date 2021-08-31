package com.plasstech.lang.d2.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.common.base.Preconditions;

public class StringTable {
  private final List<StringEntry> orderedEntries = new ArrayList<>();
  // Values sorted by descending length
  private final Set<String> values = new TreeSet<>((a, b) -> b.length() - a.length());
  private final Map<String, StringEntry> entries = new HashMap<>();
  private int index;

  // Return the entries in lexicographic order
  public List<StringEntry> orderedEntries() {
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
          int offset = parentEntry.value().lastIndexOf(childValue);
          if (offset != -1) {
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

  public void addEntry(String value) {
    orderedEntries.clear();
    values.add(value);
  }

  private String generateName(String value) {
    StringBuilder sanitizedNameValue = new StringBuilder();
    // replace all non-alphanumerics with underscore
    for (char c : value.toCharArray()) {
      if (Character.isLetterOrDigit(c)) {
        sanitizedNameValue.append(c);
      } else {
        sanitizedNameValue.append('_');
      }
    }
    return String.format("__CONST_%s_%d", sanitizedNameValue, index++);
  }

  public StringEntry lookup(String value) {
    regenerateEntries();
    Preconditions.checkState(entries.containsKey(value), "Does not contain value %s", value);
    return entries.get(value);
  }

  public int size() {
    regenerateEntries();
    return entries.size();
  }
}
