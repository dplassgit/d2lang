package com.plasstech.lang.d2.codegen;

import java.util.List;

public interface CodeGenerator<T> {

  List<T> generate();
}
