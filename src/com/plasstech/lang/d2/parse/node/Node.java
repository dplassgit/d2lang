package com.plasstech.lang.d2.parse.node;

import com.plasstech.lang.d2.codegen.Location;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/** Interface type for nodes in the parse tree. */
public interface Node {

  // Indicates it's a simple type - constant or variable.
  boolean isSimpleType();

  /** Variable type */
  VarType varType();

  void setVarType(VarType varType);

  boolean isError();

  String message();

  /** Position of this node in the input text. */
  Position position();

  /** Visitor pattern. */
  void accept(NodeVisitor visitor);

  /** Storage location (e.g., memory, register, stack) for this node */
  Location location();

  void setLocation(Location location);
}
