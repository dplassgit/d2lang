package com.plasstech.lang.d2.type;

import java.util.Set;

import com.google.common.collect.ImmutableSet;

/** The type of an expression or variable. */
public interface VarType {
  VarType BOOL = new SimpleType("BOOL", 1);
  VarType BYTE = new SimpleType("BYTE", 1);
  VarType DOUBLE = new SimpleType("DOUBLE", 8);
  VarType INT = new SimpleType("INT", 4);
  VarType LONG = new SimpleType("LONG", 8);
  VarType NULL = new NullType();
  VarType PROC = new SimpleType("PROC");
  VarType STRING = new StringType();
  VarType UNKNOWN = new UnknownType();
  VarType VOID = new SimpleType("VOID");

  /**
   * @return a name that uniquely describes this type. For example, "string",
   *         "proc:(int,string):bool", "array:int"
   */
  String name();

  /** # of bytes (on the stack) that this type occupies. For non-stack things, returns 0. */
  int size();

  default boolean isUnknown() {
    return this == UNKNOWN;
  }

  default boolean isNull() {
    return this == NULL;
  }

  default boolean isArray() {
    return false;
  }

  default boolean isRecord() {
    return false;
  }

  default boolean compatibleWith(VarType that) {
    return that == this;
  }

  final Set<VarType> NUMERIC_TYPES = ImmutableSet
      .of(VarType.BYTE, VarType.INT, VarType.LONG, VarType.DOUBLE);
  final Set<VarType> INTEGRAL_TYPES = ImmutableSet.of(VarType.BYTE, VarType.INT, VarType.LONG);

  default boolean isIntegral() {
    return INTEGRAL_TYPES.contains(this);
  }

  default boolean isNumeric() {
    return NUMERIC_TYPES.contains(this);
  }

  static void register(VarType it, String name) {
    VarTypeRegistry.register(it);
  }

  static VarType fromName(String name) {
    return VarTypeRegistry.fromName(name);
  }
}
