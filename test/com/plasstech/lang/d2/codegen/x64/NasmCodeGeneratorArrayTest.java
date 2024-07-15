package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.type.VarType;
import com.plasstech.lang.d2.type.testing.PrimitiveTypeProvider;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorArrayTest {
  // TODO: test print

  private static final String DASSERTS =
      " assertTrue:proc(b:bool) {if not b {exit 'sorry'} else {println 'true, as expected'}} "
          + "assertFalse:proc(b:bool) {if b {exit 'sorry'}  else {println 'false, as expected'}} ";

  @Test
  public void arrayDeclConstantSize(
      @TestParameter(valuesProvider = PrimitiveTypeProvider.class) VarType type)
      throws Exception {
    assertThatCompiling(String.format("x:%s[%d]", type, type.name().length()))
        .executedEqualsInterpreted();
  }

  @Test
  public void arrayDeclConstantSizeInProc(
      @TestParameter(valuesProvider = PrimitiveTypeProvider.class) VarType type) throws Exception {
    assertThatCompiling(String.format(
        "p:proc: %s { x:%s[%d] return x[0] } println p()",
        type, type, type.name().length())).executedEqualsInterpreted();
  }

  @Test
  public void arrayDeclCalculatedSize() throws Exception {
    assertThatCompiling("x:int[calc()] calc: proc:int{return 3}").executedEqualsInterpreted();
  }

  @Test
  public void arrayDeclGlobalSize() throws Exception {
    assertThatCompiling("size=3 x:string[size]").executedEqualsInterpreted();
  }

  @Test
  public void arrayDeclLocalSize() throws Exception {
    assertThatCompiling("f:proc {size=3 x:string[size]} f()").executedEqualsInterpreted();
  }

  @Test
  public void arrayGet(
      @TestParameter(valuesProvider = PrimitiveTypeProvider.class) VarType type) throws Exception {
    assertThatCompiling(String.format("x:%s[2] print x[0]", type)).executedEqualsInterpreted();
  }

  @Test
  public void arrayGetInProc(
      @TestParameter(valuesProvider = PrimitiveTypeProvider.class) VarType type) throws Exception {
    assertThatCompiling(
        String.format("p:proc {x:%s[2] print 'Should be 0 or false or null: ' println x[0]} p()",
            type))
        .executedEqualsInterpreted();
  }

  @Test
  public void arraySetString() throws Exception {
    assertThatCompiling("x:string[1] x[0]='hi' println x[0]").executedEqualsInterpreted();
  }

  @Test
  public void setNegativeIndex(
      @TestParameter boolean optimize) throws Exception {
    assertThatCompiling("x:string[1] a=2 x[a-5]='bhi'")
        .withOptimize(optimize)
        .withRuntimeError("ARRAY index must be non-negative; was -3")
        .executes();
  }

  @Test
  public void emptyArray() throws Exception {
    assertThatCompiling("x:string[0]").executedEqualsInterpreted();
  }

  @Test
  public void emptyArrayAsParam() throws Exception {
    assertThatCompiling(
        "f:proc(a:string[]): int { return length(a)} "
            + "e:string[0] "
            + "println f(['hi', 'there']) "
            + "println f(e)")
        .executedEqualsInterpreted();
  }

  @Test
  public void arraySetAndGetString() throws Exception {
    assertThatCompiling(
        "      x:string[2]\n"
            + "x[0]='hi' \n"
            + "x[1]=x[0]+ ' there' \n"
            + "println \"Should be 'hi there'\" \n"
            + "println x[1]")
        .executedEqualsInterpreted();
  }

  @Test
  public void arraySetInt() throws Exception {
    assertThatCompiling("x:int[2] x[1]=2 println x[1]").executedEqualsInterpreted();
  }

  @Test
  public void arraySetByte() throws Exception {
    assertThatCompiling("x:byte[2] x[1]=0y2 println x[1]").executedEqualsInterpreted();
  }

  @Test
  public void arraySetDouble() throws Exception {
    assertThatCompiling("x:double[2] x[1]=2.0 println x[1]").executedEqualsInterpreted();
  }

  @Test
  public void arraySetIntFromGlobal() throws Exception {
    assertThatCompiling("g=2 x:int[2] x[1]=g println x[1]").executedEqualsInterpreted();
  }

  @Test
  public void arraySetByteFromGlobal() throws Exception {
    assertThatCompiling("g=0y2 x:byte[2] x[1]=g println x[1]").executedEqualsInterpreted();
  }

  @Test
  public void arraySetDoubleFromGlobal() throws Exception {
    assertThatCompiling("g=2.0 x:double[2] x[1]=g println x[1]").executedEqualsInterpreted();
  }

  @Test
  public void arraySetIntProc() throws Exception {
    assertThatCompiling("f:proc(i:int) {x:int[2] x[i]=i+2 println x[i]} f(0) f(1)")
        .executedEqualsInterpreted();
  }

  @Test
  public void arraySetDoubleProc() throws Exception {
    assertThatCompiling("f:proc(d:double) {x:double[2] x[1]=d+1.0 println x[1]} f(0.0) f(1.0)")
        .executedEqualsInterpreted();
  }

  @Test
  public void arrayConstantAssign() throws Exception {
    assertThatCompiling("x=['hi'] print x[0]").executedEqualsInterpreted();
  }

  @Test
  public void arrayConstantCalcAssign() throws Exception {
    assertThatCompiling("x=[1, f()] f: proc: int { return 3} println 'Should print 3' print x[1]")
        .executedEqualsInterpreted();
  }

  @Test
  public void arrayLengthConstantSize() throws Exception {
    assertThatCompiling("x:int[4] println length(x)").executedEqualsInterpreted();
  }

  @Test
  public void arrayLengthConstantSizeInProc() throws Exception {
    assertThatCompiling("      p:proc {" //
        + "  x:int[4] print length(x)" //
        + "}" //
        + "p()").executedEqualsInterpreted();
  }

  @Test
  public void arrayLengthCalculatedSize() throws Exception {
    assertThatCompiling("x:int[calc()] calc: proc:int{return 3} print length(x)")
        .executedEqualsInterpreted();
  }

  @Test
  public void arrayLengthGlobalSize() throws Exception {
    assertThatCompiling("len=3 x:string[len] print length(x)").executedEqualsInterpreted();
  }

  @Test
  public void arrayLengthLocalSize() throws Exception {
    assertThatCompiling("f:proc {size=3 x:string[size] print length(x)} f()")
        .executedEqualsInterpreted();
  }

  @Test
  public void arrayParam() throws Exception {
    assertThatCompiling("       arrayParam:proc(arr:int[]) {"
        + "   println 'Should print 2' print arr[1]"
        + " }"
        + " arrayParam([1,2])").executedEqualsInterpreted();
  }

  @Test
  public void byteArrayParam() throws Exception {
    assertThatCompiling("       arrayParam:proc(arr:byte[]) {"
        + "   println 'Should print 2' print arr[1]"
        + " }"
        + " arrayParam([0y1, 0y2])").executedEqualsInterpreted();
  }

  @Test
  public void arrayAllocConstLengthNegative_error(
      @TestParameter boolean optimize) throws Exception {
    assertThatCompiling("f:proc {size=-3 x:string[size] print length(x)} f()")
        .withOptimize(optimize)
        .withRuntimeError("ARRAY size must be non-negative; was -3")
        .executes();
    assertThatCompiling("f:proc {size=-3 x:string[size+size] print length(x)} f()")
        .withOptimize(optimize)
        .withRuntimeError("ARRAY size must be non-negative; was -6")
        .executes();
  }

  @Test
  public void arrayAllocLengthNegative_runtimeError(@TestParameter boolean optimize)
      throws Exception {
    assertThatCompiling("s=-3 x:string[s]")
        .withOptimize(optimize)
        .withRuntimeError("ARRAY size must be non-negative; was -3")
        .executes();
    assertThatCompiling("x:string[size()] size: proc:int{return -3}")
        .withOptimize(optimize)
        .withRuntimeError("ARRAY size must be non-negative; was -3")
        .executes();
  }

  @Test
  public void arraySetIndexConstNegative_error(
      @TestParameter boolean optimize) throws Exception {
    assertThatCompiling("f:proc {y=-3 x:string[1] x[y] = 'hi' print length(x)} f()")
        .withOptimize(optimize)
        .withRuntimeError("ARRAY index must be non-negative; was -3")
        .executes();
  }

  @Test
  public void arraySetIndexLocalNegative_error(
      @TestParameter boolean optimize) throws Exception {
    assertThatCompiling("f:proc {y=-3 x:string[1] x[y] = 'hi' print length(x)} f()")
        .withOptimize(optimize)
        .withRuntimeError("ARRAY index must be non-negative; was -3")
        .executes();
  }

  @Test
  public void arraySetIndexLocalOOBE(
      @TestParameter boolean optimize) throws Exception {
    assertThatCompiling("f:proc {y=3 x:string[1] x[y] = 'hi' print length(x)} f()")
        .withOptimize(optimize)
        .withRuntimeError("out of bounds (length 1); was 3")
        .executes();
  }

  @Test
  public void arrayGetIndexConstNegative_error(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling("f:proc {y=-3 x:string[1] print x[y]} f()")
        .withOptimize(optimize)
        .withRuntimeError("must be non-negative; was -3")
        .executes();
  }

  @Test
  public void arrayGetIndexOOBE(@TestParameter boolean optimize) throws Exception {
    assertThatCompiling("f:proc {y=3 x:string[1] println x[y]} f()")
        .withOptimize(optimize)
        .withRuntimeError("out of bounds (length 1); was 3")
        .executes();
  }

  @Test
  public void arrayLiteral() throws Exception {
    assertThatCompiling("x=[1,2,3] print x[0]").executedEqualsInterpreted();
  }

  @Test
  public void arrayDoubleLiteral() throws Exception {
    assertThatCompiling("x=[1.0,2.0,3.0] println x[0]").executedEqualsInterpreted();
  }

  @Test
  public void arrayOfRecord() throws Exception {
    assertThatCompiling("r:record{a:string} \r\n"
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
        + "}\r\n").executedEqualsInterpreted();
  }

  @Test
  public void compareSelf() throws Exception {
    assertThatCompiling(DASSERTS //
        + "a1=[1,2,3] "
        + "assertTrue(a1 == a1) "
        + "assertFalse(a1 != a1)").executedEqualsInterpreted();
  }

  @Test
  public void compareEqual() throws Exception {
    assertThatCompiling(DASSERTS
        + "a1=[1,2,3] "
        + "a2=[1,2,3] "
        + "assertTrue(a1 == a2) "
        + "assertFalse(a1 != a2) ").executedEqualsInterpreted();
  }

  @Test
  public void compareSameSizes() throws Exception {
    assertThatCompiling(DASSERTS
        + "a1=[1,2,3] "
        + "a2=[1,2,4] "
        + "assertFalse(a1 == a2) "
        + "assertTrue(a1 != a2) ").executedEqualsInterpreted();
  }

  @Test
  public void compareDifferentSizes() throws Exception {
    assertThatCompiling(DASSERTS
        + "a1=[1,2,3] " //
        + "a2=[1,2] "
        + "assertFalse(a1 == a2) "
        + "assertTrue(a1 != a2) ").executedEqualsInterpreted();
  }

  @Test
  public void compareDifferentSizesLocals() throws Exception {
    assertThatCompiling(DASSERTS
        + "test:proc {"
        + "  a1=[1,2,3] " //
        + "  a2=[1,2] "
        + "  assertFalse(a1 == a2) "
        + "  assertTrue(a1 != a2)"
        + "}"
        + "test() ").executedEqualsInterpreted();
  }

  @Test
  public void compareDifferentSizesParams() throws Exception {
    assertThatCompiling(DASSERTS
        + "test:proc(r1:int, r2:int, r3:int) {"
        + "  a1=[1, 2, 3] " //
        + "  a2=[1, 2, 3] "
        + "  a1[0] = r1"
        + "  assertFalse(a1 == a2) "
        + "  assertTrue(a1 != a2)"
        + "}"
        + "test(2, 3, 4) ").executedEqualsInterpreted();
  }

  @Test
  public void compareParamsSame() throws Exception {
    assertThatCompiling(DASSERTS
        + "test:proc(a1:int[], a2:int[]) {"
        + "  assertTrue(a1 == a2) "
        + "  assertFalse(a1 != a2)"
        + "}"
        + "a1=[1, 2, 3] " //
        + "a2=[1, 2, 3] "
        + "test(a1, a2) ").executedEqualsInterpreted();
  }

  @Test
  public void compareParamsNotSame() throws Exception {
    assertThatCompiling(DASSERTS
        + "test:proc(a1:int[], a2:int[]) {"
        + "  assertFalse(a1 == a2) "
        + "  assertTrue(a1 != a2)"
        + "}"
        + "a1=[1, 2, 3] "
        + "a2=[1, 4] "
        + "test(a1, a2) ").executedEqualsInterpreted();
  }

  @Test
  public void compareParamsSameR8Conflict() throws Exception {
    assertThatCompiling(DASSERTS
        + "test:proc(r1:int, a1:int[], a2:int[]) {"
        + "  assertTrue(a1 == a2) "
        + "  assertFalse(a1 != a2)"
        + "}"
        + "a1=[1, 2, 3] " //
        + "a2=[1, 2, 3] "
        + "test(1, a1, a2) ").executedEqualsInterpreted();
  }

  @Test
  public void printManyArraysGlobals() throws Exception {
    String pattern = "%s=[1,2,3] println %s\n";
    String program = "";
    for (char c = 'a'; c <= 'd'; c++) {
      program += String.format(pattern, c, c);
    }
    assertThatCompiling(program).executedEqualsInterpreted();
  }

  @Test
  public void printManyArraysLocals() throws Exception {
    String pattern = "%s=[1,2,3] println %s\n";
    String program = "fun:proc {";
    for (char c = 'a'; c <= 'z'; c++) {
      program += String.format(pattern, c, c);
    }
    program += "}\n fun()";
    assertThatCompiling(program).executedEqualsInterpreted();
  }

  @Test
  public void printManyArraysParams() throws Exception {
    String pattern = "%s=[1,2,3] println %s\n";
    String program = "fun:proc(";
    char limit = 'd';
    for (char c = 'a'; c <= limit; c++) {
      program += String.format("%s:int[],", c);
    }
    program += "ignored:bool) {\n";
    for (char c = 'a'; c <= limit; c++) {
      program += String.format(pattern, c, c);
    }
    program += "}\nfun(";
    for (char c = 'a'; c <= limit; c++) {
      program += "[1],";
    }
    program += "true)\n";
    assertThatCompiling(program).executedEqualsInterpreted();
  }

  @Test
  public void assignments() throws Exception {
    String program =
        "      data:int[14]\n"
            + "data[0]=2\n"
            + "data[1]=1\n"
            + "data[2]=4\n"
            + "data[3]=5\n"
            + "data[4]=20\n"
            + "data[5]=40\n"
            + "data[6]=1\n"
            + "data[7]=9\n"
            + "data[8]=100\n"
            + "data[9]=0\n"
            + "data[10]=8\n"
            + "data[11]=6\n"
            + "data[12]=98\n"
            + "data[13]=0\n"
            + " println data";
    assertThatCompiling(program).withOptDebugLevel(2).executedEqualsInterpreted();
  }
}
