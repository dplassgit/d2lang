package com.plasstech.lang.d2.codegen;

import static com.google.common.truth.Truth.assertThat;
import static com.plasstech.lang.d2.codegen.testing.ILCodeGeneratorSubject.assertThatGenerating;

import com.plasstech.lang.d2.codegen.il.BinOp;
import com.plasstech.lang.d2.codegen.il.Op;
import com.plasstech.lang.d2.codegen.il.SysCall;
import com.plasstech.lang.d2.common.TokenType;
import java.util.List;
import java.util.function.Predicate;
import org.junit.Ignore;
import org.junit.Test;

/**
 * IMPORTANT: This test mostly validates that the ILCodeGenerator *can* generate code for the given
 * program, *not* that the code is necessarily *correct*.
 */
public class ILCodeGeneratorTest {
  @Test
  public void print() {
    assertThatGenerating("print 123").succeeds();
  }

  @Test
  public void simpleIf() {
    assertThatGenerating("i=1 j=i if 1==i {i=2 print i } ").succeeds();
  }

  @Test
  public void assignments() {
    assertThatGenerating(
            "a=3 b=-a c=b+4 d=(3-c)/(a*b+9) print c e=true f=not e g=a==b h=(a>b) or (c!=d) and e")
        .succeeds();
  }

  @Test
  public void stringAssignment() {
    assertThatGenerating("a:string a='hi' print a").succeeds();
  }

  @Test
  public void println() {
    assertThatGenerating("a='world' print 'hello, ' println a").succeeds();
  }

  @Test
  public void stringExpression() {
    assertThatGenerating("a='hi' b=a+' world'").succeeds();
  }

  @Test
  public void hugeAssignments() {
    assertThatGenerating(
            """
            a=((1 + 2) * (3 - 4) / (-5) == 6) != true
              or ((2 - 3) * (4 - 5) / (-6) < 7) == not false and
              ((3 + 4) * (5 + 6) / (-7) >= (8 % 2))
            b=(1 + 2 * 3 - 4 / 5 == 6 != true) or (2 - 3 * 4 - 5 /- 6 < 7 == not a)
              and (3 + 4 * 5 + 6 / -7 >= 8 % 2)
            """)
        .succeeds();
  }

  private static boolean containsMatchingBinOp(List<Op> ops, Predicate<BinOp> predicate) {
    boolean found = false;
    for (Op op : ops) {
      if (op instanceof BinOp binOp) {
        found |= predicate.test(binOp);
      }
    }
    return found;
  }

  @Test
  public void shortCircuitAnd() {
    List<Op> ops = assertThatGenerating("bucket=3 x = bucket==3 and bucket > 4").succeeds();
    // Assert that ops doesn't contain an AND
    assertThat(containsMatchingBinOp(ops, binOp -> binOp.operator().equals(TokenType.AND)))
        .isFalse();
  }

  @Test
  public void noShortCircuitAnd() {
    List<Op> ops = assertThatGenerating("bucket = 4 x = bucket == 3 and false").succeeds();
    // Assert that ops contains an AND
    assertThat(containsMatchingBinOp(ops, binOp -> binOp.operator().equals(TokenType.AND)))
        .isTrue();
  }

  @Test
  public void shortCircuitOr() {
    List<Op> ops = assertThatGenerating("bucket=3 x = bucket==3 or bucket > 4").succeeds();
    // Assert that ops doesn't contain an OR
    assertThat(containsMatchingBinOp(ops, binOp -> binOp.operator().equals(TokenType.OR)))
        .isFalse();
  }

  @Test
  public void noShortCircuitOr() {
    List<Op> ops = assertThatGenerating("bucket = 4 x = bucket == 3 or false").succeeds();
    // Assert that ops contains an or
    assertThat(containsMatchingBinOp(ops, binOp -> binOp.operator().equals(TokenType.OR))).isTrue();
  }

  @Test
  public void nullCoalesceSimple() {
    List<Op> ops = assertThatGenerating("a='' b=null c=a??b").succeeds();
    assertThat(
            containsMatchingBinOp(ops, binOp -> binOp.operator().equals(TokenType.NULL_COALESCE)))
        .isTrue();
  }

  @Test
  public void nullCoalesceComplex() {
    List<Op> ops = assertThatGenerating("f:proc:string{ return null} a='' c=a??f()").succeeds();
    // Assert that ops doesn't contain NULL_COALESCE
    assertThat(
            containsMatchingBinOp(ops, binOp -> binOp.operator().equals(TokenType.NULL_COALESCE)))
        .isFalse();
  }

  @Test
  public void ifStmt() {
    assertThatGenerating(
            """
            a=0
            if a==0 {
              b=1+2*3
            }
            """)
        .succeeds();
  }

  @Test
  public void ifStmts() {
    assertThatGenerating(
            """
            a=0
            if a==0 {print 1}
            elif ((-5) == 6) != true {
              b=1+2*3
            } else {
              print 2
            }
            print 3
            """)
        .succeeds();
  }

  @Test
  public void main() {
    assertThatGenerating("a=0 print a").succeeds();
  }

  @Test
  public void whileStmt() {
    assertThatGenerating("i=0 while i < 30 do i = i+1 {print i}").succeeds();
  }

  @Test
  public void whileContinue() {
    assertThatGenerating("i=0 while i < 30 do i = i+1 {if i > 10 { continue } print i} print 1")
        .succeeds();
  }

  @Test
  public void whileBreak() {
    assertThatGenerating("i=0 while i < 30 do i = i+1 {if i > 10  { break } print i} print -1")
        .succeeds();
  }

  @Test
  public void whileNestedBreak() {
    assertThatGenerating(
            """
            i=0 while i < 30 do i = i + 1 {
              j = 0 while j < 10 do j = j + 1 {
                print j
                break
              }
              if i > 10  { break }
              print i
            }
            print -1
            """)
        .succeeds();
  }

  @Test
  public void procVoid() {
    assertThatGenerating("f:proc() {print 'hi'} f()").succeeds();
  }

  @Test
  public void procInt() {
    assertThatGenerating("f:proc():int {return 3} x=f() print x").succeeds();
  }

  @Test
  public void procArg() {
    assertThatGenerating("f:proc(n:int, m:int):int {return n+m} a=3 x=f(1, a) f(2,3)").succeeds();
  }

  @Test
  public void stringIndex() {
    assertThatGenerating("a='hi' b=a[1]").succeeds();
  }

  @Test
  @Ignore
  public void stringSlice() {
    assertThatGenerating("a='abcde' b=a[1:3]").succeeds();
  }

  @Test
  public void constStringIndex() {
    assertThatGenerating("a='hi'[1]").succeeds();
  }

  @Test
  public void arrayAlloc() {
    assertThatGenerating("a:int[3]").succeeds();
  }

  @Test
  public void emptyArrayAlloc() {
    assertThatGenerating("a:int[0]").succeeds();
  }

  @Test
  public void arrayGet() {
    assertThatGenerating("a:int[3] print a[0]").succeeds();
  }

  @Test
  public void arrayLiteral() {
    assertThatGenerating("a=['a', 'b', 'c']").succeeds();
  }

  @Test
  public void arrayLiteralCalculated() {
    assertThatGenerating(
            "f:proc():string { return 'b'} b:proc() {a:string[4] a=['a', f(), 'c']} b()")
        .succeeds();
  }

  @Test
  public void stringLength() {
    assertThatGenerating("a=length('hi')").succeeds();
  }

  @Test
  public void printTwo() {
    List<Op> program = assertThatGenerating("print 'a'+'b'").succeeds();
    assertThat(
            program.stream()
                .filter(
                    op -> {
                      return op instanceof SysCall;
                    })
                .count())
        .isEqualTo(2);
  }

  @Test
  public void recordFieldSet() {
    assertThatGenerating(
            """
            rec: record {f:string i:int}
            r = new rec
            r.f = 'hi'
            """)
        .succeeds();
  }

  @Test
  public void genericRecordFieldSet() {
    assertThatGenerating(
            """
            rec: record<T> {f:T}
            r = new rec<string>
            r.f = 'hi'
            """)
        .succeeds();
  }

  @Test
  public void genericRecordFieldGet() {
    assertThatGenerating(
            """
            rec: record<T> {f:T}
            r = new rec<string>
            x = r.f
            """)
        .succeeds();
  }

  @Test
  public void bug_269_variable_with_record_name() throws Exception {
    assertThatGenerating("r: record{} r=new r").hasError("already declared as");
  }

  @Test
  public void constantRange() {
    assertThatGenerating("a=0:3").succeeds();
  }

  @Test
  public void rangeIndex() {
    assertThatGenerating("a=0:3 b=a[0] b=a[b]").succeeds();
  }

  @Test
  public void recordWithArray() {
    assertThatGenerating("rt: record{d:double ar:int[3]} x=new rt ar=x.ar ar[1]=3 print x.ar")
        .succeeds();
  }

  @Test
  public void divBy0Literal() {
    assertThatGenerating("a=1 b=a/0 println b").hasError("Division by 0");
  }

  @Test
  public void compareString() {
    assertThatGenerating("s='hi' a=s[0]=='h'").succeeds();
  }
}
