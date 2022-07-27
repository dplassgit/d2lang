package com.plasstech.lang.d2.codegen;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.type.VarType;

/*
 * what should the output of this class be?
 *
 * map string -> string entry
 * where a string entry is:
 *    synthetic name
 *    AND:
 *      string
 *      OR
 *      other string entry plus offset
 * what's "name"? how will the code generator know what to use where?
 * thoughts:
 *  * codegen asks the string finder for a list of data entries  < easy
 *  * for each string codegen asks the string finder -- based on the constant - which name to reference
 */

/**
 * Finds all String constants in the code.
 *
 * @see ConstFinder
 */
class StringFinder {

  public StringTable execute(ImmutableList<Op> code) {
    StringTable stringTable = new StringTable();
    ConstFinder<String> constFinder =
        new ConstFinder<String>(stringTable, vt -> vt == VarType.STRING);
    constFinder.find(code);
    return stringTable;
  }
}
