package com.plasstech.lang.d2.codegen;

import java.util.Map;

import com.google.common.collect.ImmutableMap;

/** Represents an absolute string constant in the data section of the nasm file. */
class StringConstant extends StringEntry {
  private static final Map<Character, Integer> ESCAPES =
      ImmutableMap.of(
          '\n', 10,
          '\r', 13,
          '\t', 9,
          '\"', 34,
          '%', 37);

  StringConstant(String name, String value) {
    super(name, value);
  }

  @Override
  public String dataEntry() {
    StringBuilder escaped = new StringBuilder();
    boolean inQuote = false;
    for (char c : value().toCharArray()) {
      Integer escapeNum = ESCAPES.get(c);
      if (escapeNum != null) {
        if (inQuote) {
          escaped.append("\", ");
        }
        escaped.append(escapeNum).append(", ");
        inQuote = false;
      } else {
        if (!inQuote) {
          escaped.append('"');
          inQuote = true;
        }
        escaped.append(c);
      }
    }
    if (inQuote) {
      escaped.append("\", ");
    }
    // this might not work for non-nasm
    return String.format("%s: db %s0", name(), escaped);
  }
}
