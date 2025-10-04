package com.plasstech.lang.d2.optimize.matcher;

import com.plasstech.lang.d2.codegen.il.Op;

public interface OpcodeOptimizer {

  Op optimize(Op source);

  boolean matches(Op source);
}
