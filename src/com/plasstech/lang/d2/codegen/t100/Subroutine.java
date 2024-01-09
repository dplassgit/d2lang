package com.plasstech.lang.d2.codegen.t100;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.plasstech.lang.d2.codegen.Trimmers;

class Subroutine {
  enum Name {
    D_print32,
    D_copy32,
    D_sub32,
    D_comp32,
    D_add32,
    D_mult8,
    D_mult32,
    D_print8,
    D_shift_left8,
    D_shift_right8,
    D_shift_left32,
    D_shift_right32,
    D_inc32,
    D_dec32,
    D_div8,
    D_bitand32,
    D_bitor32,
    D_bitxor32,
    D_bitnot32,
    D_div32
  }

  private final Name name;
  private final ImmutableList<String> code;
  private final ImmutableSet<Name> dependencies;

  Subroutine(Name name, List<String> code) {
    this.name = name;
    this.code = ImmutableList.copyOf(code);
    this.dependencies = code.stream()
        .map(Trimmers::trim)
        .filter(line -> line.startsWith("call D_"))
        .map(extern -> extern.substring(5))
        .map(dep -> Name.valueOf(dep))
        .collect(ImmutableSet.toImmutableSet());
  }

  Name name() {
    return name;
  }

  ImmutableList<String> code() {
    return code;
  }

  ImmutableSet<Name> dependencies() {
    return dependencies;
  }
}