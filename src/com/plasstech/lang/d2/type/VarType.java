package com.plasstech.lang.d2.type;

/** The type of an expression or variable. */
public interface VarType {
  VarType INT = new SimpleType("INT", 4);
  VarType STRING =
      new SimpleType("STRING", 8) {
        @Override
        public boolean compatibleWith(VarType that) {
          return super.compatibleWith(that) || that == NULL;
        }
      };
  VarType BOOL = new SimpleType("BOOL", 1);
  VarType BYTE = new SimpleType("BYTE", 1);
  // technically this is a "double" but /shrug.
  VarType FLOAT = new SimpleType("FLOAT", 8);
  VarType VOID = new SimpleType("VOID");
  VarType PROC = new SimpleType("PROC");
  VarType NULL = new NullType();

  VarType UNKNOWN =
      new VarType() {
        @Override
        public String name() {
          return "UNKNOWN";
        }

        @Override
        public int size() {
          throw new IllegalStateException("Should not try to get size of UNKNOWN");
        }

        @Override
        public String toString() {
          return name();
        }
      };

  /**
   * @return a name that uniquely describes this type. For example, "string",
   *     "proc:(int,string):bool", "array:int"
   */
  String name();

  /** # of bytes (on the stack) that this type occupies. For non-stack things, returns 0. */
  int size();

  default boolean isUnknown() {
    return this == UNKNOWN;
  }

  default boolean isNull() {
    return false;
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
}
