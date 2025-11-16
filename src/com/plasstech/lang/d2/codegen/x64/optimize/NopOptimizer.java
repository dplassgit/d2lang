package com.plasstech.lang.d2.codegen.x64.optimize;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.Trimmers;

/** Remove empty lines and lines that are only comments. */
class NopOptimizer extends Optimizer {
  @Override
  protected ImmutableList<String> doOptimize(ImmutableList<String> code) {
    ImmutableList<String> newCode =
        code.stream()
            .filter(line -> line.trim().length() > 0)
            .filter(line -> !line.trim().startsWith(";"))
            .map(line -> Trimmers.rightTrim(line))
            .collect(ImmutableList.toImmutableList());
    setChanged(newCode.size() != code.size());
    return newCode;
  }
}
