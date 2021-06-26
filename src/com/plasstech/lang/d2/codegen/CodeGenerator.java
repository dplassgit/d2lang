package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;

public interface CodeGenerator<T> {

  ImmutableList<T> generate();
}
