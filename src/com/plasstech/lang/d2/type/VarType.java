package com.plasstech.lang.d2.type;

/**
 * The type of an expression or variable.
 */
public interface VarType {
  VarType INT = new SimpleType("INT");
  VarType STRING = new SimpleType("STRING");
  VarType BOOL = new SimpleType("BOOL");
  VarType VOID = new SimpleType("VOID");
  VarType PROC = new SimpleType("PROC"); // ???
  VarType UNKNOWN = new VarType() {
    @Override
    public String name() {
      return "UNKNOWN";
    }

    @Override
    public String toString() {
      return name();
    }
  };

  /**
   * @return a name that uniquely describes this type. For example, "string",
   *         "proc:(int,string):bool", "array:int"
   */
  String name();

  default boolean isUnknown() {
    return this == UNKNOWN;
  }
  
  default boolean isArray() {
    return this instanceof ArrayType;
  }
}
