package com.plasstech.lang.d2.parse;

import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.common.Position;
import com.plasstech.lang.d2.lex.KeywordToken.KeywordType;
import com.plasstech.lang.d2.type.VarType;

public class DeclarationNode extends StatementNode {

  public final static ImmutableSet<
          KeywordType> BUILTINS = ImmutableSet.of(KeywordType.BOOL, KeywordType.INT);

  private final String varName;
  private final VarType type;

  DeclarationNode(String varName, VarType type, Position position) {
    super(Type.DECLARATION, position);
    this.varName = varName;
    this.type = type;
  }

  public VarType declaredType() {
    return type;
  }

  public String name() {
    return varName;
  }

  @Override
  public String toString() {
    return String.format("DeclNode: %s: %s", varName, type.name().toLowerCase());
  }
}
