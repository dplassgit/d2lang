package com.plasstech.lang.d2.codegen;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorArrayTest extends NasmCodeGeneratorTestBase {
  @Test
  public void arrayDecl(@TestParameter({"string", "int", "bool"}) String type) throws Exception {
    execute(String.format("x:%s[%d]", type, type.length()), "arayDecl");
  }

  @Test
  public void arrayDeclInProc(@TestParameter({"string", "int", "bool"}) String type)
      throws Exception {
    execute(
        String.format(
            "      p:proc() {" //
                + "  x:%s[%d]" //
                + "}" //
                + "p()",
            type, type.length()),
        "arayDeclInProc");
  }

  @Test
  public void arrayDeclCalculatedSize() throws Exception {
    execute("c:bool[calc()] calc: proc:int{return 3}", "boolArrayCalculatedSize");
  }

  @Test
  public void arrayGet(
      @TestParameter({
              /*"string", */
            "int", "bool"
          })
          String type)
      throws Exception {
    execute(String.format("a:%s[2] print a[0]", type), "arrayGet");
  }

  @Test
  public void arrayGetInProc(
      @TestParameter({
              /*"string", */
            "int", "bool"
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
