package com.plasstech.lang.d2.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Environment {
  private final List<String> output = new ArrayList<>();
  private final Map<String, Integer> values = new HashMap<>();

  public void setValue(String name, boolean value) {
    if (value) {
      values.put(name, 1);
    } else {
      values.put(name, 0);
    }
  }

  public void setValue(String name, int value) {
    values.put(name, value);
  }

  public Integer getValue(String name) {
    return values.get(name);
  }

  public List<String> output() {
    return output;
  }

  public void addOutput(String line) {
    output.add(line);
  }

  @Override
  public String toString() {
    return values.toString();
  }
}
