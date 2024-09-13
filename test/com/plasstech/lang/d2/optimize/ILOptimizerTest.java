package com.plasstech.lang.d2.optimize;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.optimize.testing.OptimizerSubject.assertThatInterpreting;
import static org.junit.Assert.assertThrows;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.collect.ImmutableList;
import com.google.testing.junit.testparameterinjector.TestParameter;
import com.google.testing.junit.testparameterinjector.TestParameterInjector;
import com.plasstech.lang.d2.codegen.il.DefaultOpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.OpcodeVisitor;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.codegen.il.UnaryOp;
import com.plasstech.lang.d2.common.D2RuntimeException;
import com.plasstech.lang.d2.common.TokenType;
import com.plasstech.lang.d2.interpreter.InterpreterResult;
import com.plasstech.lang.d2.testing.TestCode;

@RunWith(TestParameterInjector.class)
public class ILOptimizerTest {
  @Test
  public void plusZero() {
    assertThatInterpreting("a = 0 + 1 b = a + 0 c = 2 + 0").hasSameVariables();
  }

  @Test
  public void plusConstants() {
    assertThatInterpreting("a = 2 + 3").hasSameVariables();
  }

  @Test
  public void minusConstants() {
    assertThatInterpreting("a = 2 - 3").hasSameVariables();
  }

  @Test
  public void minusZero() {
    assertThatInterpreting("a = 2 b=a-0").hasSameVariables();
  }

  @Test
  public void zeroMinusConstant() {
    assertThatInterpreting("a = 2 b=0-a").hasSameVariables();
  }

  @Test
  public void minusItself() {
    assertThatInterpreting("minusItself:proc() {a = 2 b=a-a println b} minusItself()")
        .hasSameVariables();
  }

  @Test
  public void zeroMinus() {
    assertThatInterpreting("zeroMinus:proc() {a = 0 b=a-3 println b} zeroMinus()")
        .hasSameVariables();
  }

  @Test
  public void unaryMinus() {
    assertThatInterpreting("unaryMinus:proc() {a = 3 b=-a println b} unaryMinus()")
        .hasSameVariables();
  }

  @Test
  public void divConstants() {
    assertThatInterpreting("a = 10 / 2").hasSameVariables();
  }

  @Test
  public void divByOne() {
    assertThatInterpreting("a = 10 b=a/1").hasSameVariables();
  }

  @Test
  public void divByZero() {
    assertThrows(D2RuntimeException.class,
        () -> assertThatInterpreting("b=10 a=b/0").hasSameVariables());
  }

  @Test
  public void modConstants() {
    assertThatInterpreting("a = 14 % 5").hasSameVariables();
    assertThatInterpreting("a = 14 % 14").hasSameVariables();
  }

  @Test
  public void modByOne() {
    assertThatInterpreting("a = 14 b=a%1 println 'a' println a println 'b' println b")
        .hasSameVariables();
  }

  @Test
  public void plusStringConstants() {
    assertThatInterpreting("a = 'hi' + ' there'").hasSameVariables();
  }

  @Test
  public void timesZero() {
    assertThatInterpreting("a = 1 * 0 b = a * 0 d = 0 * 3").hasSameVariables();
  }

  @Test
  public void timesConstants() {
    assertThatInterpreting("      timesConstants:proc():int {"
        + "  a = 2 * 3 "
        + "  return a "
        + "} " //
        + "println timesConstants()").hasSameVariables();
    assertThatInterpreting("a = 1 * 3").hasSameVariables();
    assertThatInterpreting("a = 3 b=1*a c=a*1").hasSameVariables();
  }

  @Test
  public void timesPowerOfTwo() {
    assertThatInterpreting("timesPowerOfTwo:proc(a:int):int {return a*4} print timesPowerOfTwo(3)")
        .hasSameVariables();
    assertThatInterpreting("timesPowerOfTwo:proc(a:int):int {return 16*a} print timesPowerOfTwo(3)")
        .hasSameVariables();
  }

  @Test
  public void divPowerOfTwo() {
    assertThatInterpreting("divPowerOfTwo:proc(a:int):int {return a/4} print divPowerOfTwo(256)")
        .hasSameVariables();
    assertThatInterpreting("divPowerOfTwo:proc(a:int):int {return a/16} print divPowerOfTwo(1024)")
        .hasSameVariables();
  }

  @Test
  public void shift() {
    assertThatInterpreting("shift:proc(a:int):int {return a<<4} print shift(256)")
        .hasSameVariables();
    assertThatInterpreting("shift:proc(a:int):int {return a>>2} print shift(1024)")
        .hasSameVariables();
  }

  @Test
  public void shiftZero() {
    assertThatInterpreting("shiftZero:proc(a:int):int {return a<<0} print shiftZero(256)")
        .hasSameVariables();
    assertThatInterpreting("shiftZero:proc(a:int):int {return a>>0} print shiftZero(256)")
        .hasSameVariables();
  }

  @Test
  public void andConstant() {
    assertThatInterpreting("a = true b = a and  true").hasSameVariables();
    assertThatInterpreting("a = true b = true and a").hasSameVariables();
    assertThatInterpreting("a = true and true").hasSameVariables();
    assertThatInterpreting("a = true and false").hasSameVariables();
    assertThatInterpreting("a = false and true").hasSameVariables();
    assertThatInterpreting("a = false and false").hasSameVariables();
  }

  @Test
  public void orConstant() {
    assertThatInterpreting("a = true b = a or  true").hasSameVariables();
    assertThatInterpreting("a = true b = true or a").hasSameVariables();
    assertThatInterpreting("a = true or true").hasSameVariables();
    assertThatInterpreting("a = true or false").hasSameVariables();
    assertThatInterpreting("a = false or  true").hasSameVariables();
    assertThatInterpreting("a = false or false").hasSameVariables();
  }

  @Test
  public void eqIntConstant() {
    assertThatInterpreting("a = 3==3").hasSameVariables();
    assertThatInterpreting("a = 4==3").hasSameVariables();
    assertThatInterpreting("a = 3!=3").hasSameVariables();
    assertThatInterpreting("a = 4!=3").hasSameVariables();
  }

  @Test
  public void eqStringConstant() {
    assertThatInterpreting("a = 'hi' == 'bye'").hasSameVariables();
    assertThatInterpreting("a = 'hi' == 'hi'").hasSameVariables();
    assertThatInterpreting("a = 'hi' != 'bye'").hasSameVariables();
    assertThatInterpreting("a = 'hi' != 'hi'").hasSameVariables();
  }

  @Test
  public void eqBoolConstant() {
    assertThatInterpreting("a = true == true").hasSameVariables();
    assertThatInterpreting("a = true != true").hasSameVariables();
    assertThatInterpreting("a = true == false").hasSameVariables();
    assertThatInterpreting("a = true != false").hasSameVariables();
    assertThatInterpreting("a = false == false").hasSameVariables();
    assertThatInterpreting("a = false != false").hasSameVariables();
  }

  @Test
  public void ltGtIntConstant() {
    assertThatInterpreting("a = 3>3").hasSameVariables();
    assertThatInterpreting("a = 4>=3").hasSameVariables();
    assertThatInterpreting("a = 3<3").hasSameVariables();
    assertThatInterpreting("a = 4<=3").hasSameVariables();
  }

  @Test
  public void compareConstant(@TestParameter({"<", ">", ">=", "<=", "==", "!="}) String op) {
    assertThatInterpreting(String.format("b=4 println b %s 3 println 3 %s b", op, op))
        .hasSameVariables();
  }

  @Test
  public void constantPropagationTransfer() {
    assertThatInterpreting("a = 4 b = a").hasSameVariables();
  }

  @Test
  public void constantPropagationBooleans() {
    assertThatInterpreting("a = true b = a and true c = b and false d=a and b").hasSameVariables();
    assertThatInterpreting("constantPropagationBooleans:proc() {"
        + "  a = true b = a and true c = b and false d=a and b "
        + "  print a print b print c print d"
        + "} "
        + "constantPropagationBooleans()").hasSameVariables();
  }

  @Test
  public void constantAsc() {
    assertThatInterpreting("a = asc('b') b = a").hasSameVariables();
  }

  @Test
  public void constantChr() {
    assertThatInterpreting("a = chr(65) b = a").hasSameVariables();
  }

  @Test
  public void constantStringLength() {
    assertThatInterpreting("a = length('abc') b = a").hasSameVariables();
  }

  @Test
  public void constantArrayLength() {
    assertThatInterpreting("a = length([1,2,3,4]) b = a").hasSameVariables();
    assertThatInterpreting("a = length([true, false]) b = a").hasSameVariables();
    assertThatInterpreting("a = length(['a', 'b', 'c']) b = a").hasSameVariables();
  }

  @Test
  public void constantPropIf() {
    assertThatInterpreting("      constantPropIf:proc() {" //
        + "  a = 4 " //
        + "  if a == (2+2) {" //
        + "    print a"
        + "  }" //
        + "} " //
        + "constantPropIf()").hasSameVariables();
  }

  @Test
  public void constantPropReturn() {
    assertThatInterpreting("      constantPropReturn:proc():int { return 3} " //
        + "print constantPropReturn()").hasSameVariables();
  }

  @Test
  public void constantPropCall() {
    assertThatInterpreting("      constantPropCall:proc(n:int, m:int):int { return n+1} "
        + "b=4 " //
        + "print constantPropCall(4, b) " //
        + "print constantPropCall(b+2, 4+6)").hasSameVariables();
  }

  @Test
  public void multipleReturns() {
    assertThatInterpreting("      multipleReturns: proc(i: int): string {\r\n"
        + "  if i == 0 {\r\n"
        + "    return '0'\r\n"
        + "  }\r\n"
        + "  val = ''\r\n"
        + "  return val\r\n"
        + "}"
        + "println multipleReturns(314159)\r\n").hasSameVariables();
  }

  @Test
  public void deadIf() {
    assertThatInterpreting("deadIf=false a=4 if true {a=3} print a").hasSameVariables();
  }

  @Test
  public void deadWhileFalse() {
    assertThatInterpreting("deadWhileFalse=false a=4 while false {a=3} print a").hasSameVariables();
  }

  @Test
  public void deadWhile() {
    // It's not smart enough yet to detect this
    assertThatInterpreting("      deadWhile:proc() { a=4 while a>4 {a=3} print a} " //
        + "deadWhile()").hasSameVariables();
  }

  @Test
  public void deadWhileImmediateBreak() {
    assertThatInterpreting("      deadWhileImmediateBreak:proc() { a=4 while a>0 {break} print a} " //
        + "deadWhileImmediateBreak()").hasSameVariables();
  }

  @Test
  public void deadAfterReturn() {
    assertThatInterpreting("deadAfterReturn:proc(): int {return 4 a=4} print deadAfterReturn()")
        .hasSameVariables();
    assertThatInterpreting("      deadAfterReturn2:proc(a:bool): int {"
        + "  if a {return 4 a=false} return 5"
        + "} "
        + "print deadAfterReturn2(true) //"
        + "print deadAfterReturn2(false)").hasSameVariables();
  }

  @Test
  public void deadAfterExit() {
    //    TestUtils.optimizeAssertSameVariables(
    // this is broken, it should not compile.
    //        "deadAfterExit:proc(): int {exit 'no' a=4} print deadAfterExit()");
    assertThatInterpreting("      deadAfterExit2:proc(a:bool): int {\n"
        + " if a {exit 'no2' a=false } return 5\n"
        + "}\n"
        + "print deadAfterExit2(true)\n"
        + "print deadAfterExit2(false)").hasSameVariables();
  }

  @Test
  public void deadAssignment() {
    assertThatInterpreting("      deadAssignment:proc() {a=4 a=a print a} " //
        + "deadAssignment()").hasSameVariables();
  }

  @Test
  public void deadAssignments() {
    assertThatInterpreting("deadAssignments:proc(b:int):int {a=b c=b return b+1} " //
        + "print deadAssignments(3)").hasSameVariables();
  }

  @Test
  public void deadAssignmentsGlobal() {
    assertThatInterpreting("b=3 a=b c=b print a+1").hasSameVariables();
  }

  @Test
  public void incDec() {
    assertThatInterpreting("      incDec:proc(b:int):int {"
        + "  b=b+1 " // b=4
        + "  a=b*2 " // a=8
        + "  a=a-1 " // a=3
        + "  return a+b} " // 7
        + "print incDec(3)").hasSameVariables();
  }

  @Test
  public void incDecGlobals() {
    assertThatInterpreting("b=3 b=b+1 a=b*2 a=a-1 print a+1").hasSameVariables();
  }

  @Test
  public void recordLoopInvariant() {
    assertThatInterpreting(TestCode.RECORD_LOOP_INVARIANT).hasSameVariables();
  }

  @Test
  public void recordLoopNotInvariant() {
    assertThatInterpreting(TestCode.RECORD_LOOP_NOT_INVARIANT).hasSameVariables();
  }

  @Test
  public void linkedList() {
    assertThatInterpreting(TestCode.LINKED_LIST).hasSameVariables();
  }

  @Test
  public void adjacentArithmetics() {
    assertThatInterpreting("a=1 b=1+a+1 print b").hasSameVariables();
  }

  @Test
  @Ignore
  public void recordWithArray() {
    assertThatInterpreting("rt: record{d:double ar:int[3]} x=new rt ar=x.ar ar[1]=3 println x.ar")
        .hasSameVariables();
  }

  @Test
  public void stringIndex() {
    assertThatInterpreting("a='hi' b=a[1] println a println b").hasSameVariables();
  }

  @Test
  public void shortVoidLocal() {
    assertThatInterpreting("      shortVoidLocal:proc(n:int) { m = n + 1 print m } " //
        + "shortVoidLocal(3) ").hasSameVariables();
  }

  @Test
  public void arrayLiteralLength() {
    // Note: this returns the optimized result.
    InterpreterResult result =
        assertThatInterpreting("a=[1,2] println length(a)").hasSameVariables();
    OpcodeVisitor ov = new DefaultOpcodeVisitor() {
      @Override
      public void visit(SysCall op) {
        assertThat(op.arg().isConstant()).isTrue();
      }

      @Override
      public void visit(UnaryOp op) {
        assertThat(op.operator()).isNotEqualTo(TokenType.LENGTH);
      }
    };

    ImmutableList<Op> code = result.code();
    for (Op op : code) {
      op.accept(ov);
    }
  }

  @Test
  public void arrayParamLength() {
    InterpreterResult result =
        assertThatInterpreting("f:proc(a:int[]) { println length(a)} f([1,2])").hasSameVariables();
    OpcodeVisitor ov = new DefaultOpcodeVisitor() {
      private int sysCallCount = 0;

      @Override
      public void visit(SysCall op) {
        assertThat(op.arg().isConstant()).isEqualTo(sysCallCount == 0);
        sysCallCount++;
      }

      @Override
      public void visit(UnaryOp op) {
        assertThat(op.operator()).isEqualTo(TokenType.LENGTH);
      }
    };

    ImmutableList<Op> code = result.code();
    for (Op op : code) {
      op.accept(ov);
    }
  }

  @Test
  public void rangeIndex() {
    String program = "a=0:3 b=0 println a[b]";
    assertThatInterpreting(program).hasSameVariables();
  }

  @Test
  public void compareString() {
    assertThatInterpreting("s='hi' a=s[0]=='h' println a").hasSameVariables();
  }

}
