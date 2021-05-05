package com.plasstech.lang.d2.codegen;

public interface CodeGenerator<T> {

  void generate();

  void emit(T op);
}
