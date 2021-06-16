package com.plasstech.lang.d2.codegen;

import java.util.List;

import com.plasstech.lang.d2.codegen.il.Op;

public interface Optimizer {

  List<Op> optimize(List<Op> program);
}
