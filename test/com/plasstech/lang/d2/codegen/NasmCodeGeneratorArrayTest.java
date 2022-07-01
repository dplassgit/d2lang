package com.plasstech.lang.d2.codegen;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorArrayTest extends NasmCodeGeneratorTestBase {
  @Test
  @Ignore
  public void arraySet() throws Exception {
    execute("b='oh, hi' a=['hi']", "arraySet");
  }

  @Test
  @Ignore
  public void arrayAssign() throws Exception {
    execute("a=['hi'] a[0]='bye'", "arrayAssign");
  }
}
