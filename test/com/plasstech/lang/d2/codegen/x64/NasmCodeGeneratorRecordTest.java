package com.plasstech.lang.d2.codegen.x64;

import static com.plasstech.lang.d2.codegen.x64.testing.ExecutionSubject.assertThatCompiling;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorRecordTest {
  @Test
  public void alloc() throws Exception {
    assertThatCompiling("r: record{i:int s:string} x=new r").executedEqualsInterpreted();
  }

  @Test
  public void alloc_generic() throws Exception {
    assertThatCompiling("r: record<T>{i:T s:string} x=new r<int>").executedEqualsInterpreted();
  }

  @Test
  public void allocEmpty() throws Exception {
    assertThatCompiling("r: record{} x=new r").executedEqualsInterpreted();
  }

  @Test
  public void allocRecursive() throws Exception {
    assertThatCompiling("rt: record{i:int r:rt} x=new rt").executedEqualsInterpreted();
  }

  @Test
  public void setField() throws Exception {
    assertThatCompiling("rt: record{i:int s:string} x=new rt x.i=3 x.i=x.i+1")
        .executedEqualsInterpreted();
  }

  @Test
  public void declaredInProc_setField() throws Exception {
    assertThatCompiling(
            """
            f:proc:int{ rt: record{i:int s:string} x=new rt x.i=3 x.i=x.i+1 return x.i}
            println f()
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void setDoubleFieldConstant() throws Exception {
    assertThatCompiling("rt: record{d:double s:string} x=new rt x.d=3.0")
        .executedEqualsInterpreted();
  }

  @Test
  public void setDoubleField() throws Exception {
    assertThatCompiling("rt: record{d:double s:string} dd=3.0 x=new rt x.d=dd")
        .executedEqualsInterpreted();
  }

  @Test
  public void setDoubleFieldParam() throws Exception {
    assertThatCompiling(
            """
            rt: record{d:double s:string}
            f: proc(dd:double): rt {
              x=new rt x.d=dd return x
            }
            f(3.0)
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void setFieldInProc() throws Exception {
    assertThatCompiling(
            """
            rt: record{s:string i:int}
            f:proc:int {i=3 x=new rt x.i=i return i}
            print f()
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void setFieldRecordRefIsParam() throws Exception {
    assertThatCompiling(
            """
            rt: record{s:string i:int}
            f:proc(x:rt):int {i=3 x.i=i return i}
            y=new rt
            print f(y)
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void setFieldRecordIsParam() throws Exception {
    assertThatCompiling(
            """
            rt: record{s:string i:int}
            f:proc(x:rt):int {i=3 x.i=i return i}
            print f(new rt)
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void getField() throws Exception {
    assertThatCompiling("rt: record{i:int s:string} x=new rt x.i=3 print x.i")
        .executedEqualsInterpreted();
  }

  @Test
  public void getFieldDouble() throws Exception {
    assertThatCompiling("rt: record{d:double s:string} x=new rt x.d=3.0 print x.d")
        .executedEqualsInterpreted();
  }

  @Test
  public void getFieldInProc() throws Exception {
    assertThatCompiling(
            "rt: record{s:string i:int} f:proc:int {i=3 x=new rt x.i=i y=x.i return y} print f()")
        .executedEqualsInterpreted();
  }

  @Test
  public void getStringFieldInProc() throws Exception {
    assertThatCompiling(
            """
            rt: record{i:int s:string }
            f:proc:string {s='h' x=new rt x.s=s y=x.s return y}
            print f()
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void getFieldDoubleInProc() throws Exception {
    assertThatCompiling(
            """
            rt: record{s:string d:double}
            f:proc(dd:double):double {
              x=new rt
              x.d=dd
              y=x.d
              return y
            }
            print f(3.0)
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void getFieldDoubleInProcToLocal() throws Exception {
    assertThatCompiling(
            """
            rt: record{s:string d:double}
            f:proc(dd:double):double {
              x=new rt
              x.d=dd
              loc=x.d
              return loc
            }
            print f(3.0)
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void getFieldDoubleInProcToArg() throws Exception {
    assertThatCompiling(
            """
            rt: record{s:string d:double}
            f:proc(dd:double):double {
              x=new rt
              x.d=dd
              dd=x.d
              return dd
            }
            print f(3.0)
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void getFieldRecordRefIsParam() throws Exception {
    assertThatCompiling(
            """
            rt: record{s:string i:int}
            f:proc(x:rt):int {return x.i}
            y=new rt
            y.i=3 print f(y)
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void crashOnSet() throws Exception {
    assertThatCompiling(
            """
            Token: record {
              type: int
              start: Token
              end: Token
              value: String
            }
            makeToken: proc(type: int, start: Token, end: Token, text: String): Token {
              token = new Token
              token.type = type
              token.start = start
              token.end = end
              token.value = text
              print 'Made a token of value: '
              println token.value
              print 'Made a token of type: '
              println token.type
              return token
            }
            t = makeToken(1, null, null, 'keyword1')
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void nullCheck() throws Exception {
    String program =
        """
        rt: record {s:string i:int}
        a:rt a=new rt
        b:rt b = null
        a = b
        println a.s
        """;
    assertThatCompiling(program)
        .withOptimize(true)
        .hasCompileTimeError("Cannot retrieve field \"s\" of NULL RECORD");
    assertThatCompiling(program)
        .withOptimize(false)
        .withRuntimeError("Null pointer error")
        .executes();
  }

  @Test
  public void compareToNull_bug220() throws Exception {
    assertThatCompiling(
            """
            rt: record{s:string i:int}
            f:proc(x:rt):int { if x != null {return x.i} return -1}
            y=new rt
            y.i=3 print f(y)
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void compare() throws Exception {
    assertThatCompiling(
            """
            rt: record {s:string i:int}
            a=new rt a.s='hi' a.i=3
            b=new rt b.s='hi' b.i=3
            print 'a==b Should be true: ' println a==b
            if not (a==b) {exit 'assertion failure 1'}
            print 'a!=b Should be false: ' println a!=b
            if (a!=b) {exit 'assertion failure 2'}
            print 'a==a Should be true: ' println a==a
            if not (a==a) {exit 'assertion failure 3'}
            print 'b==b Should be true: ' println b==b
            if not (b==b) {exit 'assertion failure 4'}
            c=a
            print 'c==a Should be true: ' println c==a
            if not (c==a) {exit 'assertion failure 5'}
            print 'c==b Should be true: ' println c==b
            if not (c==b) {exit 'assertion failure 6'}
            print 'c!=b Should be false: ' println c!=b
            if c!=b {exit 'assertion failure 7'}
            d=new rt d.s='hi ' d.i=4
            print 'a==d Should be false: ' println a==d
            if a==d {exit 'assertion failure 8'}
            print 'a!=d Should be true: ' println a!=d
            if not (a!=d) {exit 'assertion failure 9'}
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void setArrayFieldLiteral() throws Exception {
    assertThatCompiling("rt: record{d:double ar:int[3]} x=new rt x.ar=[1,2,3] print x.ar")
        .executedEqualsInterpreted();
  }

  @Test
  public void setArrayField() throws Exception {
    assertThatCompiling(
            """
            rt: record{d:double ar:int[3]}
            x=new rt
            ar=x.ar
            ar[1]=3
            println x.ar
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void setArrayOfDoubleField_bug159() throws Exception {
    assertThatCompiling(
            """
            PlanetType: record {
              status:int
              assets:double[5]
            }
            EMPIRE=2
            f:proc:PlanetType {
                p = new PlanetType
                p.status = EMPIRE
                a = p.assets
                a[0] = 123.4 // npe
                return p
            }
            p = f()
            println p.status
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void recordOfArrays() throws Exception {
    assertThatCompiling(
            """
            PlanetType: record {
              status: int
              name: string
              assets: double[5]    // amount of each type on hand: food, fuel, parts draftable, money
              prod_ratio: int[5]   // ratio of each type of asset production
              prices: int[2]       // food, fuel (note can only buy if status=empire)
              sats_arrive: int[3]  // arrival date (in DAYS) of each satellite
            }
            EMPIRE=2
            planets:PlanetType[1]
            f:proc:PlanetType {
                p = new PlanetType
                planets[0] = p
                p.status = EMPIRE
                a = p.assets
                a[0] = 123.4 // npe
                return p
            }
            p = f()
            println p.status
            println p.assets[0]
      	    """)
        .executedEqualsInterpreted();
  }

  @Test
  public void advancedRValue_bug158() throws Exception {
    assertThatCompiling(
            """
            r1:record{bar:r2} r2:record{baz:r3[2]} r3:record{qux:string}
            foo:r1[8]
            foo7 = new r1
            foo[7] = foo7
            ar2 = new r2
            foo7.bar = ar2
            x=ar2.baz
            ar3=new r3
            ar3.qux='hi'
            x[1]=ar3
            a=4
            f:proc:int{return 1}
            bam = foo[3+a].bar.baz[f()].qux
            println bam
            if bam != 'hi' { exit 'fail, actual ' + bam}
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void loopInvariantError_bug190_field() throws Exception {
    assertThatCompiling(
            """
            r1:record{amt: double}
            f:proc {
              amt = 0.0 i = 0
              while i < 10 do i = i + 1 {
                p = new r1
                p.amt = amt
                amt = amt + 1.1
              }
            }
            f()
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void loopInvariantError_bug190_arraySetIsAGet() throws Exception {
    assertThatCompiling(
            """
            r1:record{amt: double}
            rarray:r1[10]
            f:proc {
              i = 0 amt = 1.2
              while i < 10 do i = i + 1 {
                p = new r1
                amt = amt + 1.2
                p.amt = amt
                rarray[i] = p
              }
              i = 0 while i < 10 do i = i + 1 {
                println rarray[i].amt
              }
            }
            f()
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void loopInvariantError_bug190_trim() throws Exception {
    assertThatCompiling(
            """
            trim: proc(s: string): string {
              r = ''
              i = 0 while i < length(s) do i = i + 1 {
                c = s[i]
                if c != '\n' { r = r + c }
              }
              return r
            }
            println trim('hi\n')
            """)
        .executedEqualsInterpreted();
  }

  @Test
  public void recordParam() throws Exception {
    assertThatCompiling(
            """
            r:record{s:string i:int rec:r}
            f:proc(rec:r): int {
               amt = rec.i
               return amt * 3
            }
            nr = new r
            nr.i = 100
            println f(nr)
            """)
        .executedEqualsInterpreted();
  }
}
