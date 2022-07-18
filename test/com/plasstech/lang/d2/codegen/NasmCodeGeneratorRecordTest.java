package com.plasstech.lang.d2.codegen;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.testing.junit.testparameterinjector.TestParameterInjector;

@RunWith(TestParameterInjector.class)
public class NasmCodeGeneratorRecordTest extends NasmCodeGeneratorTestBase {

  @Test
  public void alloc() throws Exception {
    execute("r: record{i:int s:string} x=new r", "alloc");
  }

  @Test
  public void allocEmpty() throws Exception {
    execute("r: record{} x=new r", "allocEmpty");
  }

  @Test
  public void allocRecursive() throws Exception {
    execute("rt: record{i:int r:rt} x=new rt", "allocRecursive");
  }

  @Test
  public void setField() throws Exception {
    execute("rt: record{i:int s:string} x=new rt x.i=3", "setField");
  }

  @Test
  public void setFieldInProc() throws Exception {
    execute(
        "rt: record{s:string i:int} f:proc:int {i=3 x=new rt x.i=i return i} print f()",
        "setFieldInProc");
  }

  @Test
  public void setFieldRecordRefIsParam() throws Exception {
    execute(
        "      rt: record{s:string i:int} "
            + "f:proc(x:rt):int {i=3 x.i=i return i} "
            + "y=new rt "
            + "print f(y)",
        "setFieldRecordRefIsParam");
  }

  @Test
  public void setFieldRecordIsParam() throws Exception {
    execute(
        "      rt: record{s:string i:int} "
            + "f:proc(x:rt):int {i=3 x.i=i return i} "
            + "print f(new rt)",
        "setFieldRecordIsParam");
  }

  @Test
  public void getField() throws Exception {
    execute("rt: record{i:int s:string} x=new rt x.i=3 print x.i", "getField");
  }

  @Test
  public void getFieldInProc() throws Exception {
    execute(
        "rt: record{s:string i:int} f:proc:int {i=3 x=new rt x.i=i return x.i} print f()",
        "getFieldInProc");
  }

  @Test
  public void getFieldRecordRefIsParam() throws Exception {
    execute(
        "      rt: record{s:string i:int} "
            + "f:proc(x:rt):int {return x.i} "
            + "y=new rt "
            + "y.i=3 print f(y)",
        "getFieldRecordRefIsParam");
  }
}
