package com.plasstech.lang.d2.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.plasstech.lang.d2.codegen.Location;

public class Environment {
  private final List<String> output = new ArrayList<>();
  private final Map<String, Object> values = new HashMap<>();
  private final Environment parent;

  Environment() {
    parent = null;
  }

  private Environment(Environment parent) {
    this.parent = parent;
  }

  Environment spawn() {
    return new Environment(this);
  }

  public Environment parent() {
    return parent;
  }

  public void setValue(Location location, Object value) {
    values.put(location.name(), value);
  }

  public void setValue(Location location, boolean value) {
    if (value) {
      setValue(location, 1);
    } else {
      setValue(location, 0);
    }
  }

  public Object getValue(String name) {
    Object value = values.get(name);
    if (value == null && parent() != null) {
      return parent().getValue(name);
    }
    return value;
  }

  public Map<String, Object> variables() {
    Map<String, Object> variablesOnly = new HashMap<>();
    for (Map.Entry<String, Object> value : values.entrySet()) {
      if (!value.getKey().startsWith("__")) {
        // not a temp
        variablesOnly.put(value.getKey(), value.getValue());
      }
    }
    return variablesOnly;
  }

  public List<String> output() {
    return output;
  }

  public void addOutput(String line) {
    output.add(line);
  }

  @Override
  public String toString() {
    if (parent != null) {
      return values.toString() + " Parent: " + parent.toString();
    } else {
      return values.toString();
    }
  }
}
