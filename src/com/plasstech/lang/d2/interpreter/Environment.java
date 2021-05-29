package com.plasstech.lang.d2.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.plasstech.lang.d2.codegen.il.Location;

public class Environment {
  private final List<String> output = new ArrayList<>();
  private final Map<String, Object> values = new HashMap<>();

  public void setValue(Location location, Object value) {
    // TODO: can think about mangling here
    values.put(location.name(), value);
  }

  public void setValue(Location location, boolean value) {
    if (value) {
      setValue(location, 1);
    } else {
      setValue(location, 0);
    }
  }

  public void setValue(String name, int value) {
    values.put(name, value);
  }

  public Object getValue(String name) {
    // TODO: can think about mangling here
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
