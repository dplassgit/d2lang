package com.plasstech.lang.d2.parse.node;

public interface LValueNode extends Node {
  String name();

  void accept(Visitor visitor);

  interface Visitor {
    void visit(FieldSetNode node);

    void visit(VariableSetNode node);

    void visit(ArraySetNode arraySetNode);
  }
}
