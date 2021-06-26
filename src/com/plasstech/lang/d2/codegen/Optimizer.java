package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;

public interface Optimizer {

  ImmutableList<Op> optimize(ImmutableList<Op> program);
}
