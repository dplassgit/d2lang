package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.VarType;

/**
 * Finds all String constants in the code.
 *
 * @see ConstFinder
 */
public class StringFinder {
  public StringTable execute(ImmutableList<Op> code) {
    StringTable stringTable = new StringTable();
    ConstFinder<String> constFinder =
        new ConstFinder<String>(stringTable, vt -> vt == VarType.STRING);
    constFinder.find(code);
    return stringTable;
  }
}
