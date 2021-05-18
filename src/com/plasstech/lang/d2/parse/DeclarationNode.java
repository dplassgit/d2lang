package com.plasstech.lang.d2.parse;

import com.google.common.collect.ImmutableMap;
import com.plasstech.lang.d2.common.NodeVisitor;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.lex.KeywordToken.KeywordType;
import com.plasstech.lang.d2.type.VarType;

/**
 * Declare a variable, e.g., "foo:int"
 */
public class DeclarationNode extends StatementNode {

  public final static ImmutableMap<KeywordType, VarType> BUILTINS = ImmutableMap.of( //
          KeywordType.INT, VarType.INT, //
          KeywordType.BOOL, VarType.BOOL, //
          KeywordType.STRING, VarType.STRING, //
          KeywordType.PROC, VarType.PROC); // TODO I hate this.

  private final String varName;

  DeclarationNode(String varName, VarType type, Position position) {
    super(position);
    this.varName = varName;
    setVarType(type);
  }

  public String name() {
    return varName;
  }

  @Override
  public void accept(NodeVisitor visitor) {
    visitor.visit(this);
  }

  @Override
  public String toString() {
    return String.format("DeclNode: %s: %s", varName, varType().name().toLowerCase());
  }
}
