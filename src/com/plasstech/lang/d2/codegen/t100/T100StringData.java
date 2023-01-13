package com.plasstech.lang.d2.codegen.t100;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Joiner;
import com.plasstech.lang.d2.codegen.ConstEntry;

/** Represents an absolute string data in the data section of a t100 .as file. */
class T100StringData extends ConstEntry<String> {

  // I don't love this
  T100StringData(ConstEntry<String> backing) {
    super(backing.name(), backing.value());
  }

  T100StringData(String name, String value) {
    super(name, value);
  }

  @Override
  public String dataEntry() {
    // hex values of each char.
    List<String> hexValues = new ArrayList<>();
    for (char c : value().toCharArray()) {
      hexValues.add(String.format("0x%02x", (int) c));
      if (c == '\n') {
        // crlf
        hexValues.add(String.format("0x%02x", (int) '\r'));
      }
    }
    hexValues.add("0x00");

    return String.format("%s: db %s", name(), Joiner.on(',').join(hexValues));
  }
}
