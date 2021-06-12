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
  private final String name;

  Environment() {
    name = "Root";
    parent = null;
  }

  private Environment(Environment parent) {
    this.parent = parent;
    this.name = "Child of " + parent.name;
  }

  Environment spawn() {
    return new Environment(this);
  }

  public Environment parent() {
    return parent;
  }

  public void setValue(Location location, Object value) {
    System.out.printf("env %s: Setting %s to %s\n", this.name, location.name(), value);
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
    System.out.printf("env %s: Getting value of %s\n", this.name, name);
    Object value = values.get(name);
    if (value == null && parent() != null) {
      System.out.printf("Looking in parent for value of %s\n", name);
      return parent().getValue(name);
    }
    System.out.printf("env %s: Got value of %s: %s\n", this.name, name, value);
    return value;
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
