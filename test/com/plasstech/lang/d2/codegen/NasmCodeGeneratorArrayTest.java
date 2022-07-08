package com.plasstech.lang.d2.codegen;

import static org.junit.Assume.assumeTrue;

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
  public void arraySetString() throws Exception {
    execute("x:string[1] x[0]='hi' println x[0]", "arraySetString");
  }

  @Test
  public void arraySetInt() throws Exception {
    execute("x:int[2] x[1]=2 println x[1]", "arraySetInt");
  }

  @Test
  public void arraySetIntFromGlobal() throws Exception {
    execute("g=2 x:int[2] x[1]=g println x[1]", "arraySetInt");
  }

  @Test
  public void arraySetIntProc() throws Exception {
    execute("f:proc(i:int) {x:int[2] x[i]=i+2 println x[i]} f(0) f(1)", "arraySetIntProc");
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
  public void arrayLengthNegative_error() throws Exception {
    // If it's not optimized, the size constant won't be propagated.
    assumeTrue(optimize);
    assertGenerateError(
        "f:proc() {size=-3 x:string[size] print length(x)} f()", "must be positive; was -3");
    assertGenerateError(
        "f:proc() {size=-3 x:string[size-size] print length(x)} f()", "must be positive; was 0");
  }

  @Test
  public void arraySetIndexNegative_error() throws Exception {
    // If it's not optimized, the size constant won't be propagated.
    assumeTrue(optimize);
    assertGenerateError(
        "f:proc() {y=-3 x:string[1] x[y] = 'hi' print length(x)} f()",
        "must be non-negative; was -3");
  }

  @Test
  public void arrayGetIndexNegative_error() throws Exception {
    // If it's not optimized, the size constant won't be propagated.
    assumeTrue(optimize);
    assertGenerateError(
        "f:proc() {y=-3 x:string[1] print x[y]} f()", "must be non-negative; was -3");
  }

  @Test
  public void dumbSort() throws Exception {
    execute(
        "        MAX=99999 "
            + "  data:int[7] "
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
