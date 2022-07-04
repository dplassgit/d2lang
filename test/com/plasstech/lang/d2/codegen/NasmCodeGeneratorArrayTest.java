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
    execute("i:int[3]", "intArrayDecl");
    execute("b:bool[4]", "boolArrayDecl");
    execute("c:bool[calc()] calc: proc:int{return 3}", "boolArrayCalculatedSize");
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
