package com.plasstech.lang.d2.optimize;

import static org.junit.Assert.fail;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.plasstech.lang.d2.InterpreterExecutor;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.testing.TestUtils;

public class ILOptimizerTest {
  @Test
  public void plusZero() {
    TestUtils.optimizeAssertSameVariables("a = 0 + 1 b = a + 0 c = 2 + 0");
  }

  @Test
  public void plusConstants() {
    TestUtils.optimizeAssertSameVariables("a = 2 + 3");
  }

  @Test
  public void minusConstants() {
    TestUtils.optimizeAssertSameVariables("a = 2 - 3");
  }

  @Test
  public void minusZero() {
    TestUtils.optimizeAssertSameVariables("a = 2 b=a-0");
  }

  @Test
  public void zeroMinusConstant() {
    TestUtils.optimizeAssertSameVariables("a = 2 b=0-a");
  }

  @Test
  public void minusItself() {
    TestUtils.optimizeAssertSameVariables(
        "minusItself:proc() {a = 2 b=a-a println b} minusItself()");
  }

  @Test
  public void zeroMinus() {
    TestUtils.optimizeAssertSameVariables("zeroMinus:proc() {a = 0 b=a--3 println b} zeroMinus()");
  }

  @Test
  public void unaryMinus() {
    TestUtils.optimizeAssertSameVariables("unaryMinus:proc() {a = 3 b=-a println b} unaryMinus()");
  }

  @Test
  public void divConstants() {
    TestUtils.optimizeAssertSameVariables("a = 10 / 2");
  }

  @Test
  public void divByOne() {
    TestUtils.optimizeAssertSameVariables("a = 10 b=a/1");
  }

  @Test
  public void divByZero() {
    InterpreterExecutor ee = new InterpreterExecutor("a = 1 / 0");
    try {
      ee.execute();
      fail("Should fail with division by 0");
    } catch (ArithmeticException expected) {
    }

    ImmutableList<Op> optimized = new ILOptimizer().optimize(ee.state().ilCode());
    try {
      ee.execute(ee.state().addOptimizedCode(optimized));
      fail("Should fail with division by 0");
    } catch (ArithmeticException expected) {
    }
  }

  @Test
  public void modConstants() {
    TestUtils.optimizeAssertSameVariables("a = 14 % 5");
    TestUtils.optimizeAssertSameVariables("a = 14 % 14");
  }

  @Test
  public void modByOne() {
    TestUtils.optimizeAssertSameVariables("a = 14 b=a%1");
  }

  @Test
  public void plusStringConstants() {
    TestUtils.optimizeAssertSameVariables("a = 'hi' + ' there'");
  }

  @Test
  public void timesZero() {
    TestUtils.optimizeAssertSameVariables("a = 1 * 0 b = a * 0 d = 0 * 3");
  }

  @Test
  public void timesConstants() {
    TestUtils.optimizeAssertSameVariables(
        "      timesConstants:proc():int {"
            + "  a = 2 * 3 "
            + "  return a "
            + "} " //
            + "timesConstants()");
    TestUtils.optimizeAssertSameVariables("a = 1 * 3");
    TestUtils.optimizeAssertSameVariables("a = 3 b=1*a c=a*1");
  }

  @Test
  public void timesPowerOfTwo() {
    TestUtils.optimizeAssertSameVariables(
        "timesPowerOfTwo:proc(a:int):int {return a*4} print timesPowerOfTwo(3)");
    TestUtils.optimizeAssertSameVariables(
        "timesPowerOfTwo:proc(a:int):int {return 16*a} print timesPowerOfTwo(3)");
  }

  @Test
  public void divPowerOfTwo() {
    TestUtils.optimizeAssertSameVariables(
        "divPowerOfTwo:proc(a:int):int {return a/4} print divPowerOfTwo(256)");
    TestUtils.optimizeAssertSameVariables(
        "divPowerOfTwo:proc(a:int):int {return a/16} print divPowerOfTwo(1024)");
  }

  @Test
  public void shift() {
    TestUtils.optimizeAssertSameVariables("shift:proc(a:int):int {return a<<4} print shift(256)");
    TestUtils.optimizeAssertSameVariables("shift:proc(a:int):int {return a>>2} print shift(1024)");
  }

  @Test
  public void shiftZero() {
    TestUtils.optimizeAssertSameVariables(
        "shiftZero:proc(a:int):int {return a<<0} print shiftZero(256)");
    TestUtils.optimizeAssertSameVariables(
        "shiftZero:proc(a:int):int {return a>>0} print shiftZero(256)");
  }

  @Test
  public void andConstant() {
    TestUtils.optimizeAssertSameVariables("a = true b = a and  true");
    TestUtils.optimizeAssertSameVariables("a = true b = true and a");
    TestUtils.optimizeAssertSameVariables("a = true and true");
    TestUtils.optimizeAssertSameVariables("a = true and false");
    TestUtils.optimizeAssertSameVariables("a = false and true");
    TestUtils.optimizeAssertSameVariables("a = false and false");
  }

  @Test
  public void orConstant() {
    TestUtils.optimizeAssertSameVariables("a = true b = a or  true");
    TestUtils.optimizeAssertSameVariables("a = true b = true or a");
    TestUtils.optimizeAssertSameVariables("a = true or true");
    TestUtils.optimizeAssertSameVariables("a = true or false");
    TestUtils.optimizeAssertSameVariables("a = false or  true");
    TestUtils.optimizeAssertSameVariables("a = false or false");
  }

  @Test
  public void eqIntConstant() {
    TestUtils.optimizeAssertSameVariables("a = 3==3");
    TestUtils.optimizeAssertSameVariables("a = 4==3");
    TestUtils.optimizeAssertSameVariables("a = 3!=3");
    TestUtils.optimizeAssertSameVariables("a = 4!=3");
  }

  @Test
  public void eqStringConstant() {
    TestUtils.optimizeAssertSameVariables("a = 'hi' == 'bye'");
    TestUtils.optimizeAssertSameVariables("a = 'hi' == 'hi'");
    TestUtils.optimizeAssertSameVariables("a = 'hi' != 'bye'");
    TestUtils.optimizeAssertSameVariables("a = 'hi' != 'hi'");
  }

  @Test
  public void eqBoolConstant() {
    TestUtils.optimizeAssertSameVariables("a = true == true");
    TestUtils.optimizeAssertSameVariables("a = true != true");
    TestUtils.optimizeAssertSameVariables("a = true == false");
    TestUtils.optimizeAssertSameVariables("a = true != false");
    TestUtils.optimizeAssertSameVariables("a = false == false");
    TestUtils.optimizeAssertSameVariables("a = false != false");
  }

  @Test
  public void ltGtIntConstant() {
    TestUtils.optimizeAssertSameVariables("a = 3>3");
    TestUtils.optimizeAssertSameVariables("a = 4>=3");
    TestUtils.optimizeAssertSameVariables("a = 3<3");
    TestUtils.optimizeAssertSameVariables("a = 4<=3");
  }

  @Test
  public void constantPropagationTransfer() {
    TestUtils.optimizeAssertSameVariables("a = 4 b = a");
  }

  @Test
  public void constantPropagationBooleans() {
    TestUtils.optimizeAssertSameVariables("a = true b = a and true c = b and false d=a and b");
    TestUtils.optimizeAssertSameVariables(
        "constantPropagationBooleans:proc() {"
            + "  a = true b = a and true c = b and false d=a and b "
            + "  print a print b print c print d"
            + "} "
            + "constantPropagationBooleans()");
  }

  @Test
  public void constantAsc() {
    TestUtils.optimizeAssertSameVariables("a = asc('b') b = a");
  }

  @Test
  public void constantChr() {
    TestUtils.optimizeAssertSameVariables("a = chr(65) b = a");
  }

  @Test
  public void constantStringLength() {
    TestUtils.optimizeAssertSameVariables("a = length('abc') b = a");
  }

  @Test
  public void constantArrayLength() {
    TestUtils.optimizeAssertSameVariables("a = length([1,2,3,4]) b = a");
    TestUtils.optimizeAssertSameVariables("a = length([true, false]) b = a");
    TestUtils.optimizeAssertSameVariables("a = length(['a', 'b', 'c']) b = a");
  }

  @Test
  public void constantPropIf() {
    TestUtils.optimizeAssertSameVariables(
        "      constantPropIf:proc() {" //
            + "  a = 4 " //
            + "  if a == (2+2) {" //
            + "    print a"
            + "  }" //
            + "} " //
            + "constantPropIf()");
  }

  @Test
  public void constantPropReturn() {
    TestUtils.optimizeAssertSameVariables(
        "      constantPropReturn:proc():int { return 3} " //
            + "print constantPropReturn()");
  }

  @Test
  public void constantPropCall() {
    TestUtils.optimizeAssertSameVariables(
        "      constantPropCall:proc(n:int, m:int):int { return n+1} "
            + "b=4 " //
            + "print constantPropCall(4, b) " //
            + "print constantPropCall(b+2, 4+6)");
  }

  @Test
  public void multipleReturns() {
    TestUtils.optimizeAssertSameVariables(
        "      multipleReturns: proc(i: int): string {\r\n"
            + "  if i == 0 {\r\n"
            + "    return '0'\r\n"
            + "  }\r\n"
            + "  val = ''\r\n"
            + "  return val\r\n"
            + "}"
            + "println multipleReturns(314159)\r\n");
  }

  @Test
  public void deadIf() {
    TestUtils.optimizeAssertSameVariables("deadIf=false a=4 if true {a=3} print a");
  }

  @Test
  public void deadWhileFalse() {
    TestUtils.optimizeAssertSameVariables("deadWhileFalse=false a=4 while false {a=3} print a");
  }

  @Test
  public void deadWhile() {
    // It's not smart enough yet to detect this
    TestUtils.optimizeAssertSameVariables(
        "      deadWhile:proc() { a=4 while a>4 {a=3} print a} " //
            + "deadWhile()");
  }

  @Test
  public void deadWhileImmediateBreak() {
    TestUtils.optimizeAssertSameVariables(
        "      deadWhileImmediateBreak:proc() { a=4 while a>0 {break} print a} " //
            + "deadWhileImmediateBreak()");
  }

  @Test
  public void deadAfterReturn() {
    TestUtils.optimizeAssertSameVariables(
        "deadAfterReturn:proc(): int {return 4 a=4} print deadAfterReturn()");
    TestUtils.optimizeAssertSameVariables(
        "      deadAfterReturn2:proc(a:bool): int {"
            + "  if a {return 4 a=false} return 5"
            + "} "
            + "print deadAfterReturn2(true) //"
            + "print deadAfterReturn2(false)");
  }

  @Test
  public void deadAfterExit() {
    TestUtils.optimizeAssertSameVariables(
        "deadAfterExit:proc(): int {exit 'no' a=4} print deadAfterExit()");
    TestUtils.optimizeAssertSameVariables(
        "deadAfterExit2:proc(a:bool): int {"
            + " if a {exit 'no2'} a=false return 5"
            + "} "
            + "print deadAfterExit2(true) print deadAfterExit2(false)");
  }

  @Test
  public void deadAssignment() {
    TestUtils.optimizeAssertSameVariables(
        "      deadAssignment:proc() {a=4 a=a print a} " //
            + "deadAssignment()");
  }

  @Test
  public void deadAssignments() {
    TestUtils.optimizeAssertSameVariables(
        "deadAssignments:proc(b:int):int {a=b c=b return a+1} " //
            + "print deadAssignments(3)");
  }

  @Test
  public void deadAssignmentsGlobal() {
    TestUtils.optimizeAssertSameVariables("b=3 a=b c=b print a+1");
  }

  @Test
  public void incDec() {
    TestUtils.optimizeAssertSameVariables(
        "incDec:proc(b:int):int {b=b+1 a=b*2 a=a-1 return a+b} print incDec(3)");
    TestUtils.optimizeAssertSameVariables("b=3 b=b+1 a=b*2 a=a-1 print a+1");
  }

  @Test
  public void recordLoopInvariant() {
    TestUtils.optimizeAssertSameVariables(TestUtils.RECORD_LOOP_INVARIANT);
  }

  @Test
  public void recordLoopNotInvariant() {
    TestUtils.optimizeAssertSameVariables(TestUtils.RECORD_LOOP_NOT_INVARIANT);
  }

  @Test
  public void linkedList() {
    TestUtils.optimizeAssertSameVariables(TestUtils.LINKED_LIST);
  }
}
