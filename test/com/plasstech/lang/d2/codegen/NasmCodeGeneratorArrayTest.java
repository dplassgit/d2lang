package com.plasstech.lang.d2.codegen;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorArrayTest extends NasmCodeGeneratorTestBase {
  @Test
  public void arrayDeclConstantSize(@TestParameter({"string", "int", "bool"}) String type)
      throws Exception {
    execute(String.format("x:%s[%d]", type, type.length()), "arrayDeclConstantSize");
  }

  @Test
  public void arrayDeclConstantSizeInProc(@TestParameter({"string", "int", "bool"}) String type)
      throws Exception {
    execute(
        String.format(
            "      p:proc(): %s {" //
                + "  x:%s[%d] return x[0]" //
                + "}" //
                + "p()",
            type, type, type.length()),
        "arrayDeclConstantSizeInProc");
  }

  @Test
  public void arrayDeclCalculatedSize() throws Exception {
    execute("c:int[calc()] calc: proc:int{return 3}", "arrayCalculatedSize");
  }

  @Test
  public void arrayDeclGlobalSize() throws Exception {
    execute("size=3 string_arr:string[size]", "arrayDeclGlobalSize");
  }

  @Test
  public void arrayDeclLocalSize() throws Exception {
    execute("f:proc() {size=3 string_arr:string[size]} f()", "arrayDeclLocalSize");
  }

  @Test
  public void arrayGet(
      @TestParameter({
            /*"string", */
            "int",
            "bool"
          })
          String type)
      throws Exception {
    execute(String.format("a:%s[2] print a[0]", type), "arrayGet");
  }

  @Test
  public void arrayGetInProc(
      @TestParameter({
            /*"string", */
            "int",
            "bool"
          })
          String type)
      throws Exception {
    execute(String.format("p:proc() {a:%s[2] print a[0]} p()", type), "arrayGetInProc");
  }

  @Test
  @Ignore
  public void arraySet() throws Exception {
    execute("a:string[1] a[0]='hi'", "arraySet");
  }

  @Test
  @Ignore
  public void arrayConstantAssign() throws Exception {
    execute("a=['hi']'", "arrayConstantAssign");
  }
}
