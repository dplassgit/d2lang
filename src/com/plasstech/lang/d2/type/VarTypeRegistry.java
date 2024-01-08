package com.plasstech.lang.d2.type;

import java.util.HashMap;
import java.util.Map;

class VarTypeRegistry {
  private static final Map<String, VarType> REGISTRY = new HashMap<>();

  static void register(VarType type) {
    REGISTRY.put(type.name(), type);
  }

  static VarType fromName(String name) {
    return REGISTRY.get(name);
  }
}
