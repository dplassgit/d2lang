package com.plasstech.lang.d2.codegen.x64;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.phase.PhaseName;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorArrayTest extends NasmCodeGeneratorTestBase {
  // TODO: test print

  private static final String DASSERTS =
      "      assertTrue:proc(b:bool) {if not b {exit 'sorry'} else {println 'true, as expected'}} "
          + "assertFalse:proc(b:bool) {if b {exit 'sorry'}  else {println 'false, as expected'}} ";

  @Test
  public void arrayDeclConstantSize(
      @TestParameter({"string", "int", "bool", "double"}) String type)
      throws Exception {
    execute(String.format("x:%s[%d]", type, type.length()), "arrayDeclConstantSize" + type);
  }

  @Test
  public void arrayDeclConstantSizeInProc(
      @TestParameter({"string", "int", "bool", "double", "byte"}) String type) throws Exception {
    execute(
        String.format(
            "      p:proc(): %s {" //
                + "  x:%s[%d] return x[0]" //
                + "}" //
                + "p()",
            type, type, type.length()),
        "arrayDeclConstantSizeInProc" + type);
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
      @TestParameter(
        {
            /* "string", */
            "int",
            "bool",
            "double",
            "byte"
        }
      ) String type)
      throws Exception {
    execute(String.format("x:%s[2] print x[0]", type), "arrayGet" + type);
  }

  @Test
  public void arrayGetInProc(
      @TestParameter(
        {
            /* "string", */
            "int",
            "bool",
            "double",
            "byte"
        }
      ) String type)
      throws Exception {
    execute(
        String.format("p:proc() {x:%s[2] println 'Should be 0 or false' print x[0]} p()", type),
        "arrayGetInProc");
  }

  @Test
  public void arraySetString() throws Exception {
    execute("x:string[1] x[0]='hi' println x[0]", "arraySetString");
  }

  @Test
  public void emptyArray() throws Exception {
    execute("x:string[0]", "emptyArray");
  }

  @Test
  public void emptyArrayAsParam() throws Exception {
    execute("f:proc(a:string[]): int { return length(a)} "
        + "e:string[0] "
        + "println f(['hi', 'there']) "
        + "println f(e)",
        "emptyArrayAsParam");
  }

  @Test
  public void arraySetAndGetString() throws Exception {
    execute(
        "x:string[2]\r\n"
            + "x[0]=\"hi\"\r\n"
            + "x[1]=x[0]+ \" there\"\r\n"
            + "println \"Should be 'hi there'\"\r\n"
            + "println x[1]\r\n",
        "arraySetAndGetString");
  }

  @Test
  public void arraySetInt() throws Exception {
    execute("x:int[2] x[1]=2 println x[1]", "arraySetInt");
  }

  @Test
  public void arraySetByte() throws Exception {
    execute("x:byte[2] x[1]=0y2 println x[1]", "arraySetByte");
  }

  @Test
  public void arraySetDouble() throws Exception {
    execute("x:double[2] x[1]=2.0 println x[1]", "arraySetDouble");
  }

  @Test
  public void arraySetIntFromGlobal() throws Exception {
    execute("g=2 x:int[2] x[1]=g println x[1]", "arraySetIntFromGlobal");
  }

  @Test
  public void arraySetByteFromGlobal() throws Exception {
    execute("g=0y2 x:byte[2] x[1]=g println x[1]", "arraySetByteFromGlobal");
  }

  @Test
  public void arraySetDoubleFromGlobal() throws Exception {
    execute("g=2.0 x:double[2] x[1]=g println x[1]", "arraySetDoubleFromGlobal");
  }

  @Test
  public void arraySetIntProc() throws Exception {
    execute("f:proc(i:int) {x:int[2] x[i]=i+2 println x[i]} f(0) f(1)", "arraySetIntProc");
  }

  @Test
  public void arraySetDoubleProc() throws Exception {
    execute(
        "f:proc(d:double) {x:double[2] x[1]=d+1.0 println x[1]} f(0.0) f(1.0)",
        "arraySetDoubleProc");
  }

  @Test
  public void arrayConstantAssign() throws Exception {
    execute("x=['hi'] print x[0]", "arrayConstantAssign");
  }

  @Test
  public void arrayConstantCalcAssign() throws Exception {
    execute(
        "x=[1, f()] f: proc(): int { return 3} println 'Should print 3' print x[1]",
        "arrayConstantCalcAssign");
  }

  @Test
  public void arrayLengthConstantSize() throws Exception {
    execute("x:int[4] println length(x)", "arrayLengthConstantSizeInt");
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
    execute("len=3 x:string[len] print length(x)", "arrayLengthGlobalSize");
  }

  @Test
  public void arrayLengthLocalSize() throws Exception {
    execute("f:proc() {size=3 x:string[size] print length(x)} f()", "arrayLengthLocalSize");
  }

  @Test
  public void arrayParam() throws Exception {
    execute(
        "       arrayParam:proc(arr:int[]) {"
            + "   println 'Should print 2' print arr[1]"
            + " }"
            + " arrayParam([1,2])",
        "arrayParam");
  }

  @Test
  public void byteArrayParam() throws Exception {
    execute(
        "       arrayParam:proc(arr:byte[]) {"
            + "   println 'Should print 2' print arr[1]"
            + " }"
            + " arrayParam([0y1, 0y2])",
        "byteArrayParam");
  }

  @Test
  public void arrayAllocConstLengthNegative_error() throws Exception {
    // If it's not optimized, the size constant won't be propagated.
    assertGenerateError(
        "f:proc() {size=-3 x:string[size] print length(x)} f()",
        "ARRAY size must be non-negative; was -3", true, PhaseName.ASM_CODEGEN);
    assertGenerateError(
        "f:proc() {size=-3 x:string[size+size] print length(x)} f()",
        "ARRAY size must be non-negative; was -6", true, PhaseName.ASM_CODEGEN);
  }

  @Test
  public void arrayAllocLengthNegative_runtimeError() throws Exception {
    // If optimized, the proc or constant and it will degenerate to the previous test.
    assertRuntimeError("s=-3 x:string[s]", "arrayAllocLengthNegative",
        "ARRAY size must be non-negative; was -3");
    assertRuntimeError("x:string[size()] size: proc():int{return -3}",
        "arrayAllocCalLengthNegative", "ARRAY size must be non-negative; was -3");
  }

  @Test
  public void arraySetIndexConstNegative_error() throws Exception {
    // If it's not optimized, the size constant won't be propagated.
    assertGenerateError(
        "f:proc() {y=-3 x:string[1] x[y] = 'hi' print length(x)} f()",
        "ARRAY index must be non-negative; was -3", true, PhaseName.ASM_CODEGEN);
  }

  @Test
  public void arraySetIndexLocalNegative_error() throws Exception {
    // If it's not optimized, the size constant won't be propagated.
    assertRuntimeError("f:proc() {y=-3 x:string[1] x[y] = 'hi' print length(x)} f()",
        "arraySetIndexLocalNegative_error", "ARRAY index must be non-negative; was -3");
  }

  @Test
  public void arraySetIndexLocalOOBE() throws Exception {
    // If it's not optimized, the size constant won't be propagated.
    assertRuntimeError("f:proc() {y=3 x:string[1] x[y] = 'hi' print length(x)} f()",
        "arraySetIndexLocalOOBE", "out of bounds (length 1); was 3");
  }

  @Test
  public void arrayGetIndexConstNegative_error() throws Exception {
    assertGenerateError(
        "f:proc() {y=-3 x:string[1] print x[y]} f()", "ARRAY index must be non-negative; was -3",
        true, PhaseName.ASM_CODEGEN);
    assertRuntimeError(
        "f:proc() {y=-3 x:string[1] print x[y]} f()",
        "arrayGetIndexLocalNegative",
        "must be non-negative; was -3");
  }

  @Test
  public void arrayGetIndexOOBE() throws Exception {
    assertRuntimeError("f:proc() {y=3 x:string[1] print x[y]} f()", "arrayGetIndexConstOOBE",
        "out of bounds (length 1); was 3");
  }

  @Test
  public void arrayLiteral() throws Exception {
    execute("x=[1,2,3] print x[0]", "arrayLiteral");
  }

  @Test
  public void arrayDoubleLiteral() throws Exception {
    execute("x=[1.0,2.0,3.0] println x[0]", "arrayDoubleLiteral");
  }

  @Test
  public void arrayOfRecord() throws Exception {
    execute(
        "r:record{a:string} \r\n"
            + "rs:r[2]\r\n"
            + "rs[1] = new r\r\n"
            + "tr = rs[1]\r\n"
            + "tr.a='hi'\r\n"
            + "println \"Should be hi\"\r\n"
            + "println rs[1].a // will this work? no\r\n"
            + "println tr.a\r\n"
            + "\r\n"
            + "println \"Should be null\"\r\n"
            + "if rs[0] == null {\r\n"
            + "  println \"null\"\r\n"
            + "}\r\n"
            + "\r\n"
            + "println \"Should be not null\"\r\n"
            + "if rs[1] != null {\r\n"
            + "  println \"not null\"\r\n"
            + "}\r\n",
        "arrayOfRecord");
  }

  @Test
  public void compareSelf() throws Exception {
    execute(
        DASSERTS //
            + "a1=[1,2,3] "
            + "assertTrue(a1 == a1) "
            + "assertFalse(a1 != a1)",
        "compare");
  }

  @Test
  public void compareEqual() throws Exception {
    execute(
        DASSERTS
            + "a1=[1,2,3] "
            + "a2=[1,2,3] "
            + "assertTrue(a1 == a2) "
            + "assertFalse(a1 != a2) ",
        "compare");
  }

  @Test
  public void compareSameSizes() throws Exception {
    execute(
        DASSERTS
            + "a1=[1,2,3] "
            + "a2=[1,2,4] "
            + "assertFalse(a1 == a2) "
            + "assertTrue(a1 != a2) ",
        "compare");
  }

  @Test
  public void compareDifferentSizes() throws Exception {
    execute(
        DASSERTS
            + "a1=[1,2,3] " //
            + "a2=[1,2] "
            + "assertFalse(a1 == a2) "
            + "assertTrue(a1 != a2) ",
        "compare");
  }

  @Test
  public void compareDifferentSizesLocals() throws Exception {
    execute(
        DASSERTS
            + "test:proc {"
            + "  a1=[1,2,3] " //
            + "  a2=[1,2] "
            + "  assertFalse(a1 == a2) "
            + "  assertTrue(a1 != a2)"
            + "}"
            + "test() ",
        "compare");
  }

  @Test
  public void compareDifferentSizesParams() throws Exception {
    execute(
        DASSERTS
            + "test:proc(r1:int, r2:int, r3:int) {"
            + "  a1=[1, 2, 3] " //
            + "  a2=[1, 2, 3] "
            + "  a1[0] = r1"
            + "  assertFalse(a1 == a2) "
            + "  assertTrue(a1 != a2)"
            + "}"
            + "test(2, 3, 4) ",
        "compare");
  }

  @Test
  public void compareParamsSame() throws Exception {
    execute(
        DASSERTS
            + "test:proc(a1:int[], a2:int[]) {"
            + "  assertTrue(a1 == a2) "
            + "  assertFalse(a1 != a2)"
            + "}"
            + "a1=[1, 2, 3] " //
            + "a2=[1, 2, 3] "
            + "test(a1, a2) ",
        "compare");
  }

  @Test
  public void compareParamsNotSame() throws Exception {
    execute(
        DASSERTS
            + "test:proc(a1:int[], a2:int[]) {"
            + "  assertFalse(a1 == a2) "
            + "  assertTrue(a1 != a2)"
            + "}"
            + "a1=[1, 2, 3] "
            + "a2=[1, 4] "
            + "test(a1, a2) ",
        "compare");
  }

  @Test
  public void compareParamsSameR8Conflict() throws Exception {
    execute(
        DASSERTS
            + "test:proc(r1:int, a1:int[], a2:int[]) {"
            + "  assertTrue(a1 == a2) "
            + "  assertFalse(a1 != a2)"
            + "}"
            + "a1=[1, 2, 3] " //
            + "a2=[1, 2, 3] "
            + "test(1, a1, a2) ",
        "compare");
  }

  @Test
  public void printManyArraysGlobals() throws Exception {
    String pattern = "%s=[1,2,3] println %s\n";
    String program = "";
    for (char c = 'a'; c <= 'z'; c++) {
      program += String.format(pattern, c, c);
    }
    execute(program, "printMany");
  }

  @Test
  public void printManyArraysLocals() throws Exception {
    String pattern = "%s=[1,2,3] println %s\n";
    String program = "fun:proc {";
    for (char c = 'a'; c <= 'z'; c++) {
      program += String.format(pattern, c, c);
    }
    program += "}\n fun()";
    execute(program, "printManyLocals");
  }

  @Test
  @Ignore // this runs out of registers for the arguments
  public void printManyArraysParams() throws Exception {
    String pattern = "%s=[1,2,3] println %s\n";
    String program = "fun:proc(";
    for (char c = 'a'; c <= 'z'; c++) {
      program += String.format("%s:int[],", c);
    }
    program += "ignored:bool) {\n";
    for (char c = 'a'; c <= 'z'; c++) {
      program += String.format(pattern, c, c);
    }
    program += "}\nfun(";
    for (char c = 'a'; c <= 'z'; c++) {
      program += "[1],";
    }
    program += "true)\n";
    execute(program, "printManyLocals");
  }

}
