package com.plasstech.lang.d2.codegen;

import org.junit.Test;

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
}
