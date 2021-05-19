package com.plasstech.lang.d2.parse;

import com.plasstech.lang.d2.codegen.il.Location;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.type.VarType;

/**
 * Interface type for nodes in the parse tree.
 */
public interface Node {

  // Indicates it's a simple type - constant or variable.
  boolean isSimpleType();

  VarType varType();

  void setVarType(VarType varType);

  boolean isError();

  Position position();

  void accept(NodeVisitor visitor);

  void setLocation(Location location);

  Location location();
}
