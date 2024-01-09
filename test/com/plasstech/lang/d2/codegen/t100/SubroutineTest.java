package com.plasstech.lang.d2.codegen.t100;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.codegen.t100.Subroutine.Name;

public class SubroutineTest {

  @Test
  public void noDependencies() {
    Subroutine noDeps = new Subroutine(Name.D_add32, ImmutableList.of("  MOV A, D"));
    assertThat(noDeps.dependencies()).isEmpty();
  }

  @Test
  public void oneDep() {
    Subroutine oneDep = new Subroutine(Name.D_add32, ImmutableList.of("call D_inc32"));
    assertThat(oneDep.dependencies()).containsExactly(Name.D_inc32);
  }

  @Test
  public void duplicateDeps() {
    Subroutine dupDeps = new Subroutine(Name.D_add32,
        ImmutableList.of("call D_inc32", " call D_inc32 ", "  call D_inc32  ; comment"));
    assertThat(dupDeps.dependencies()).containsExactly(Name.D_inc32);
  }

  @Test
  public void multipleDeps() {
    Subroutine multipleDeps = new Subroutine(Name.D_div32,
        ImmutableList.of("D_div32:", "call D_inc32", " call D_shift_left32"));
    assertThat(multipleDeps.dependencies()).containsExactly(Name.D_inc32, Name.D_shift_left32);
  }
}
