package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Test;

public class NasmCodeGeneratorLogicFlowTest {
  @Test
  public void ifPrint() throws Exception {
    assertThatCompiling("a=3 if a > 1 {print a}").executedEqualsInterpreted();
  }

  @Test
  public void fib() throws Exception {
    assertThatCompiling(
            """
            n1 = 0
            n2 = 1
            nth = 0
            i=1 while i <= 10 do i = i + 1 {
              nth = n1 + n2
              n1 = n2
              n2 = nth
            }
            print nth
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void fibRecursive() throws Exception {
    assertThatCompiling(
            """
            recursive_fib: proc(n: int) : int {
              if n <= 1 {
                return n
              } else {
                return recursive_fib(n - 1) + recursive_fib(n - 2)
              }
            }
            println recursive_fib(10)
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void fib0() throws Exception {
    assertThatCompiling(
            """
            n=10
            n1 = 0
            n2 = 1
            nth = 0
            i=1 while i <= n do i = i+1 {
              nth = n1 + n2
              n1 = n2
              n2 = nth
              print i
              print "th fib: "
              println nth
            }
            println '' // NOTE: cannot just do PRINT (no expression)...
            print "Final fib: "
            println nth
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void fact() throws Exception {
    assertThatCompiling(
            """
            fact = 1
            i=1 while i <= 10 do i = i + 1 {
              fact = fact * i
            }
            println fact
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void nullCompare() throws Exception {
    assertThatCompiling(
            """
            s=''
            if null != null { println 'This should never happen'}
            if null == null { println 'null'}
            if null != s { println 'null'}
            if null == s { println 'This should never happen'}
            if s != null { println 'null'}
            if s == null { println 'This should never happen'}
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void commonSubexpressionElimination() throws Exception {
    assertThatCompiling(
            """
            f:proc(a:int, b:int):int {
              c=a+b
              d=a+b+c
              println c
              return d
            }
            e=f(1, 2)
            println e+1
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void evilNames() throws Exception {
    assertThatCompiling(
            """
            _f:proc(D_a:int, b:int):int {
              c=D_a+b
              D_d=D_a+b+c
              println c
              return D_d
            }
            D_after_proc__f_0=_f(1, 2)
            println D_after_proc__f_0+1
            """)
        .executedEqualsInterpreted();
  }
}
