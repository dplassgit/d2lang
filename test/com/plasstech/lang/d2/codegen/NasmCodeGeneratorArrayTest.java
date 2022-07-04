package com.plasstech.lang.d2.codegen;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorArrayTest extends NasmCodeGeneratorTestBase {
  @Test
  public void arrayDecl() throws Exception {
    execute("a:string[2]", "stringArrayDecl");
    //    execute("a:int[2]", "intArrayDecl");
    //    execute("a:bool[2]", "boolArrayDecl");
  }

  @Test
  @Ignore
  public void arraySet() throws Exception {
    execute("a:string[1] a[0]='hi'", "arraySet");
  }

  @Test
  @Ignore
  public void arrayAssign() throws Exception {
    execute("a=['hi']'", "arrayAssign");
  }
}
