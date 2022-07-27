package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.VarType;

/**
 * Finds all double constants in the code.
 *
 * @see ConstFinder
 */
class DoubleFinder {
  DoubleTable execute(ImmutableList<Op> code) {
    DoubleTable doubleTable = new DoubleTable();
    ConstFinder<Double> constFinder =
        new ConstFinder<Double>(doubleTable, vt -> vt == VarType.DOUBLE);
    constFinder.find(code);
    return doubleTable;
  }
}
