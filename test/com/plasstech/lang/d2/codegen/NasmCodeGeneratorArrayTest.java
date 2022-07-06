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
    execute("x:int[calc()] calc: proc:int{return 3}", "arrayCalculatedSize");
  }

  @Test
  public void arrayDeclGlobalSize() throws Exception {
    execute("size=3 x:string[size]", "arrayDeclGlobalSize");
  }

  @Test
  public void arrayDeclLocalSize() throws Exception {
    execute("f:proc() {size=3 x:string[size]} f()", "arrayDeclLocalSize");
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
    execute(String.format("x:%s[2] print x[0]", type), "arrayGet");
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
    execute(String.format("p:proc() {x:%s[2] print x[0]} p()", type), "arrayGetInProc");
  }

  @Test
  @Ignore
  public void arraySet() throws Exception {
    execute("x:string[1] x[0]='hi'", "arraySet");
  }

  @Test
  @Ignore
  public void arrayConstantAssign() throws Exception {
    execute("x=['hi']'", "arrayConstantAssign");
  }

  @Test
  public void arrayLengthConstantSize() throws Exception {
    execute("x:int[4] print length(x)", "arrayLengthConstantSize");
  }

  @Test
  public void arrayLengthConstantSizeInProc() throws Exception {
    execute(
        "      p:proc() {" //
            + "  x:int[4] print length(x)" //
            + "}" //
            + "p()",
        "arrayLengthConstantSizeInProc");
  }

  @Test
  public void arrayLengthCalculatedSize() throws Exception {
    execute("x:int[calc()] calc: proc:int{return 3} print length(x)", "arrayLengthCalculatedSize");
  }

  @Test
  public void arrayLengthGlobalSize() throws Exception {
    execute("size=3 x:string[size] print length(x)", "arrayLengthGlobalSize");
  }

  @Test
  public void arrayLengthLocalSize() throws Exception {
    execute("f:proc() {size=3 x:string[size] print length(x)} f()", "arrayLengthLocalSize");
  }

  @Test
  @Ignore("Won't work until array set is implemented")
  public void dumbSort() throws Exception {
    execute(
        "        MAX=99999 "
            + "      data:int[7] "
            + "  data[0]=2 "
            + "  data[1]=1 "
            + "  data[2]=4 "
            + "  data[3]=8 "
            + "  data[4]=6 "
            + "  data[5]=98 "
            + "  data[6]=0 "
            + "  min=MAX "
            + "  last_min = -MAX "
            + "  // 1. find next element greater than min \r\n"
            + "  j = 0 while j < length(data) do j = j + 1 { "
            + "    min=MAX "
            + "    i = 0 while i < length(data) do i = i + 1 { "
            + "      if data[i] > last_min and data[i] < min { "
            + "        min = data[i] "
            + "      } "
            + "    } "
            + "    println min "
            + "    // now need to find something greater than min \r\n"
            + "    last_min = min "
            + "  }",
        "dumbsort");
  }
}
