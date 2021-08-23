package com.plasstech.lang.d2.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.plasstech.lang.d2.codegen.Location;

public class Environment {
  // this is a unique sentinel object
  private static final Object NULL =
      new Object() {
        @Override
        public String toString() {
          return "__nullenv__";
        }

        @Override
        public int hashCode() {
          return toString().hashCode();
        }
      };
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
    if (value == null) {
      // Sentinel
      values.put(location.name(), NULL);
    } else {
      values.put(location.name(), value);
    }
  }

  public Object getValue(String name) {
    Object value = values.get(name);
    if (value == NULL) {
      // Sentinel
      return null;
    } else if (value == null && parent() != null) {
      return parent().getValue(name);
    }
    return value;
  }

  public Map<String, Object> variables() {
    Map<String, Object> variablesOnly = new HashMap<>();
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      if (!entry.getKey().startsWith("__")) {
        // not a temp
        Object value = entry.getValue();
        if (value == NULL) {
          value = null;
        }
        variablesOnly.put(entry.getKey(), entry.getValue());
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
